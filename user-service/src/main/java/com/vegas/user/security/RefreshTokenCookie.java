package com.vegas.user.security;

import com.vegas.user.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Cookie с refresh-токеном. Все флаги — про безопасность:
 * - HttpOnly: JavaScript страницы (в том числе внедрённый через XSS) cookie не прочитает;
 * - Secure: только по HTTPS (в проде);
 * - SameSite=Strict: браузер не пошлёт cookie с чужого сайта — защита от CSRF;
 * - Path=/api/auth: cookie уходит только на эндпоинты входа/обновления/выхода, а не с каждым запросом.
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "vegas_refresh";
    private static final String PATH = "/api/auth";

    private final JwtProperties properties;

    public RefreshTokenCookie(JwtProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, properties.refreshTokenTtl());
    }

    /** Удалить cookie: то же имя и путь, пустое значение и Max-Age=0 */
    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
