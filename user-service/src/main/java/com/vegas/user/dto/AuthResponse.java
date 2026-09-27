package com.vegas.user.dto;

import java.util.UUID;

/**
 * Ответ на регистрацию и логин.
 * Фронт сохраняет accessToken и шлёт его в каждом запросе: Authorization: Bearer <accessToken>.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,     // секунд до истечения токена
        UUID userId
) {
    public static AuthResponse bearer(String accessToken, long expiresIn, UUID userId) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, userId);
    }
}
