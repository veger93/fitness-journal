package com.vegas.user.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.user.dto.UserResponse;
import com.vegas.user.entity.EmailCode;
import com.vegas.user.entity.EmailCode.Purpose;
import com.vegas.user.entity.RefreshToken;
import com.vegas.user.entity.User;
import com.vegas.user.event.OutboxService;
import com.vegas.user.mapper.UserMapper;
import com.vegas.user.repository.EmailCodeRepository;
import com.vegas.user.repository.RefreshTokenRepository;
import com.vegas.user.repository.UserRepository;
import com.vegas.user.security.SecureTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Коды из 6 цифр в письме (docs/account.md): подтверждение email и "Забыли пароль?".
 *
 * Правила: код живёт 15 минут, действует только последний отправленный, не больше 5 неверных попыток,
 * повторно отправить — не чаще раза в минуту. В базе — SHA-256 от "userId:код", не сам код.
 */
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    static final Duration CODE_TTL = Duration.ofMinutes(15);
    static final Duration RESEND_AFTER = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;
    private final UserMapper userMapper;
    private final Clock clock;

    // ---------- подтверждение email ----------

    /** После регистрации и получения аккаунта от тренера — сразу, без кнопки (пауза "раз в минуту" не нужна) */
    @Transactional
    public void sendVerificationOnSignUp(User user) {
        issue(user, Purpose.VERIFY_EMAIL);
    }

    /** "Отправить код ещё раз" */
    @Transactional
    public void resendVerification(UUID userId) {
        User user = findUser(userId);
        if (user.isEmailVerified()) {
            throw new BusinessException("Email уже подтверждён");
        }
        requireResendPause(userId, Purpose.VERIFY_EMAIL);
        issue(user, Purpose.VERIFY_EMAIL);
    }

    /** noRollbackFor: неверный код — ошибка 400, но счётчик попыток всё равно должен сохраниться */
    @Transactional(noRollbackFor = BadRequestException.class)
    public UserResponse confirmEmail(UUID userId, String code) {
        User user = findUser(userId);
        if (user.isEmailVerified()) {
            return userMapper.toResponse(user);
        }
        EmailCode emailCode = activeCode(userId, Purpose.VERIFY_EMAIL)
                // код на старый адрес (email потом исправили) новый адрес не подтверждает
                .filter(c -> c.getEmail().equals(user.getEmail()))
                .orElseThrow(() -> new BadRequestException("Код устарел — отправьте новый"));
        check(emailCode, userId, code);
        user.markEmailVerified(clock.instant());
        return userMapper.toResponse(user);
    }

    /** Исправить email: ошибся при регистрации. Новый адрес снова не подтверждён — сразу шлём на него код */
    @Transactional
    public UserResponse changeEmail(UUID userId, String newEmail, String password) {
        User user = findUser(userId);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            // 400, а не 401: 401 фронт понял бы как "сессия истекла" и разлогинил бы человека
            throw new BadRequestException("Неверный пароль");
        }
        String email = User.normalizeEmail(newEmail);
        if (email.equals(user.getEmail())) {
            return userMapper.toResponse(user);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Пользователь с таким email уже зарегистрирован");
        }
        user.changeEmail(email);
        // notification-service обновит адрес для писем (событие то же, что при регистрации — он делает upsert)
        outboxService.userRegistered(user);
        issue(user, Purpose.VERIFY_EMAIL);
        return userMapper.toResponse(user);
    }

    // ---------- "Забыли пароль?" ----------

    /**
     * Ответ всегда одинаковый, есть такой email или нет: иначе по этой форме можно было бы
     * узнавать, кто зарегистрирован в Vegas. Код уходит только на ПОДТВЕРЖДЁННЫЙ email.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(User.normalizeEmail(email))
                .filter(User::isEmailVerified)
                .filter(user -> !recentlySent(user.getId(), Purpose.RESET_PASSWORD))
                .ifPresent(user -> issue(user, Purpose.RESET_PASSWORD));
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(User.normalizeEmail(email))
                .orElseThrow(() -> new BadRequestException("Неверный код"));
        EmailCode emailCode = activeCode(user.getId(), Purpose.RESET_PASSWORD)
                .orElseThrow(() -> new BadRequestException("Код устарел — запросите новый"));
        check(emailCode, user.getId(), code);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // пароль меняют, когда забыли или подозревают чужой вход — закрываем все входы на всех устройствах
        refreshTokenRepository.revokeAllOfUser(user.getId(), RefreshToken.RevokeReason.PASSWORD_RESET, clock.instant());
    }

    // ---------- общее ----------

    private void issue(User user, Purpose purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant now = clock.instant();
        Instant expiresAt = now.plus(CODE_TTL);
        codeRepository.save(new EmailCode(user.getId(), purpose, user.getEmail(), hash(user.getId(), code), now, expiresAt));
        outboxService.emailCodeRequested(user, code, purpose.name(), expiresAt);
    }

    private void check(EmailCode emailCode, UUID userId, String code) {
        if (!emailCode.getCodeHash().equals(hash(userId, code))) {
            emailCode.failedAttempt();
            int left = EmailCode.MAX_ATTEMPTS - emailCode.getAttempts();
            throw new BadRequestException(left > 0
                    ? "Неверный код. Осталось попыток: " + left
                    : "Неверный код. Попытки закончились — отправьте новый");
        }
        emailCode.markUsed(clock.instant());
    }

    /** Действует только последний код, и только пока он активен */
    private Optional<EmailCode> activeCode(UUID userId, Purpose purpose) {
        return codeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .filter(c -> c.isActive(clock.instant()));
    }

    private boolean recentlySent(UUID userId, Purpose purpose) {
        return codeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .filter(c -> c.getCreatedAt().isAfter(clock.instant().minus(RESEND_AFTER)))
                .isPresent();
    }

    private void requireResendPause(UUID userId, Purpose purpose) {
        codeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .map(c -> Duration.between(clock.instant(), c.getCreatedAt().plus(RESEND_AFTER)).toSeconds())
                .filter(seconds -> seconds > 0)
                .ifPresent(seconds -> {
                    throw new BusinessException("Отправить код снова можно через " + seconds + " сек");
                });
    }

    /** id в хеше — "соль": одинаковые коды у разных людей дают разные хеши */
    static String hash(UUID userId, String code) {
        return SecureTokens.sha256(userId + ":" + code);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}
