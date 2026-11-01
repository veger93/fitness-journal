package com.vegas.user.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.user.dto.AuthResponse;
import com.vegas.user.dto.LoginRequest;
import com.vegas.user.dto.RegisterRequest;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.event.OutboxService;
import com.vegas.user.exception.InvalidCredentialsException;
import com.vegas.user.exception.InvalidRefreshTokenException;
import com.vegas.user.repository.UserProfileRepository;
import com.vegas.user.repository.UserRepository;
import com.vegas.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Регистрация и вход по email + паролю.
 * @RequiredArgsConstructor (Lombok) создаёт конструктор для final-полей -> Spring внедряет зависимости через него.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OutboxService outboxService;
    private final RefreshTokenService refreshTokenService;
    private final EmailCodeService emailCodeService;
    private final Clock clock;

    /**
     * @Transactional: пользователь и его профиль сохраняются вместе.
     * Если что-то упадёт посередине — откатится всё, "пользователя без профиля" не останется.
     */
    @Transactional
    public AuthTokens register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Пользователь с таким email уже зарегистрирован");
        }

        User user = User.local(email, passwordEncoder.encode(request.password()), request.displayName());
        userRepository.save(user);
        // пустой профиль сразу: онбординг потом только обновляет его
        userProfileRepository.save(new UserProfile(user));
        // notification-service узнает email нового пользователя
        outboxService.userRegistered(user);
        // код подтверждения email — сразу в письме; пользоваться приложением можно и до подтверждения
        emailCodeService.sendVerificationOnSignUp(user);

        return issueTokens(user);
    }

    /** Не readOnly: при входе в БД записывается новый refresh-токен */
    @Transactional
    public AuthTokens login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                // у Google-пользователя пароля нет -> войти по паролю нельзя
                .filter(u -> u.getPasswordHash() != null)
                // matches сам хеширует введённый пароль с солью из хеша и сравнивает
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return issueTokens(user);
    }

    /**
     * Обмен refresh-токена из cookie на новую пару. Пароль не нужен — поэтому refresh-токен
     * и хранится там, где JavaScript (а значит, и XSS) до него не дотянется.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthTokens refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        // пользователя могли удалить, пока жил токен
        User user = userRepository.findById(rotation.userId())
                // демо закончилось: новый токен не выдаём — фронт выйдет на лендинг (docs/demo.md, "без сирот")
                .filter(u -> !u.isDemoExpired(clock.instant()))
                .orElseThrow(InvalidRefreshTokenException::new);
        return new AuthTokens(accessResponse(user), rotation.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    /** package-private: ещё нужен ClaimService — клиент получил аккаунт и сразу вошёл */
    AuthTokens issueTokens(User user) {
        return new AuthTokens(accessResponse(user), refreshTokenService.issueNewFamily(user.getId()));
    }

    private AuthResponse accessResponse(User user) {
        return AuthResponse.bearer(
                jwtService.generateAccessToken(user),
                jwtService.getAccessTokenTtl().toSeconds(),
                user.getId()
        );
    }
}
