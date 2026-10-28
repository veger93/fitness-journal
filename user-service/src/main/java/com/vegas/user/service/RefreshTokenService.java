package com.vegas.user.service;

import com.vegas.user.config.JwtProperties;
import com.vegas.user.entity.RefreshToken;
import com.vegas.user.entity.RefreshToken.RevokeReason;
import com.vegas.user.exception.InvalidRefreshTokenException;
import com.vegas.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh-токены с ротацией.
 *
 * Токен — 32 случайных байта (не JWT): серверу не нужно ничего в нём читать, только найти по хешу.
 * Каждый refresh одноразовый: обменяли — старый отозван (ROTATED), выдан новый той же семьи.
 * Если кто-то предъявил уже обменянный токен — значит, у кого-то есть копия (кража cookie):
 * отзываем всю семью, и вору, и владельцу придётся войти заново.
 *
 * Исключение — "льготные" 30 секунд: две вкладки или двойной запрос React могут обменять один и тот же токен
 * почти одновременно. Это не кража, поэтому выдаём ещё один новый токен, а семью не трогаем.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final Clock clock;

    /** Новая семья — при входе или регистрации. Возвращает сам токен (он уйдёт в cookie, в БД — только хеш). */
    @Transactional
    public String issueNewFamily(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    /**
     * Обменять refresh-токен на новый. Возвращает пользователя и новый токен.
     * Транзакция одна: отзыв старого и выдача нового либо происходят вместе, либо не происходят.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class) // отзыв семьи при краже должен сохраниться
    public Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken token = find(rawToken);

        if (token.isRevoked()) {
            return rotateRevoked(token, now);
        }
        if (token.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }
        token.revoke(RevokeReason.ROTATED, now);
        return new Rotation(token.getUserId(), issue(token.getUserId(), token.getFamilyId()));
    }

    /** Предъявили уже отозванный токен: либо параллельный обмен (выдаём новый), либо отказ — а при подозрении на кражу ещё и тревога */
    private Rotation rotateRevoked(RefreshToken token, Instant now) {
        if (token.getRevokedReason() != RevokeReason.ROTATED) {
            throw new InvalidRefreshTokenException(); // после выхода или уже поднятой тревоги — просто нет
        }
        boolean justRotated = token.getRevokedAt().plus(REUSE_GRACE).isAfter(now);
        if (!justRotated) {
            log.warn("Повторное использование refresh-токена, семья {} отозвана", token.getFamilyId());
            repository.revokeFamily(token.getFamilyId(), RevokeReason.REUSE, now);
            throw new InvalidRefreshTokenException();
        }
        // обменяли только что — вторая вкладка или двойной запрос. Но если с тех пор вышли, новый не выдаём
        if (!repository.existsByFamilyIdAndRevokedAtIsNull(token.getFamilyId())) {
            throw new InvalidRefreshTokenException();
        }
        return new Rotation(token.getUserId(), issue(token.getUserId(), token.getFamilyId()));
    }

    /** Выход: отзываем всю семью — этот вход (устройство) больше не обновится. Неизвестный токен — молча игнорируем. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), RevokeReason.LOGOUT, clock.instant()));
    }

    private RefreshToken find(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
    }

    private String issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        // base64url без "=" — безопасно для cookie
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(userId, hash(raw), familyId, clock.instant().plus(properties.refreshTokenTtl())));
        return raw;
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 есть в любой JVM", e);
        }
    }

    /** Результат обмена: чей токен и какой новый выдан */
    public record Rotation(UUID userId, String refreshToken) {
    }
}
