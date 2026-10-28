package com.vegas.user.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Настройки JWT из application.yml (блок app.jwt).
 * Spring сам сопоставит access-token-ttl -> accessTokenTtl и "1h" -> Duration.
 * @Validated: если secret не задан — сервис упадёт при старте с понятной ошибкой, а не при первом логине.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration accessTokenTtl,   // короткий: 15 минут. Украли — вред ограничен временем
        @NotNull Duration refreshTokenTtl,  // длинный: дни. Лежит в httpOnly-cookie, JavaScript его не видит
        boolean cookieSecure                // Secure-флаг cookie: только по HTTPS. Локально (http) — false
) {
}
