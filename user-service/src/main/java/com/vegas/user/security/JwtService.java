package com.vegas.user.security;

import com.vegas.user.config.JwtProperties;
import com.vegas.user.entity.Role;
import com.vegas.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Выпуск и проверка JWT.
 *
 * JWT = header.payload.signature. Payload (claims) НЕ зашифрован — его может прочитать любой,
 * поэтому никаких паролей и персональных данных туда не кладём. Подпись гарантирует,
 * что токен выпустили мы и его не меняли.
 */
@Component
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        // hmacShaKeyFor бросит WeakKeyException, если ключ короче 256 бит — сервис не стартует со слабым ключом
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())          // "sub" — кому выдан токен
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))                  // "iat"
                .expiration(Date.from(now.plus(accessTokenTtl))) // "exp" — после него токен недействителен
                .signWith(key)                             // алгоритм (HS512) jjwt выберет по длине ключа
                .compact();
    }

    /**
     * Проверяет подпись и срок действия.
     * Optional.empty() — токен поддельный, испорченный или просроченный.
     */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class))
            ));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }
}
