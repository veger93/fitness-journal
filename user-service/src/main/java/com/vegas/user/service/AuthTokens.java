package com.vegas.user.service;

import com.vegas.user.dto.AuthResponse;

/**
 * Что выдаём при входе: access-токен уходит в теле ответа (его держит фронт в памяти),
 * refresh-токен — в httpOnly-cookie (его ставит контроллер, в JSON он не попадает).
 */
public record AuthTokens(AuthResponse response, String refreshToken) {
}
