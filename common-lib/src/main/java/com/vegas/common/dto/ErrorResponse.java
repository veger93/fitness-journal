package com.vegas.common.dto;

import java.time.Instant;

/**
 * Единый формат ошибки для всех сервисов — фронт всегда получает одинаковый JSON.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now());
    }
}
