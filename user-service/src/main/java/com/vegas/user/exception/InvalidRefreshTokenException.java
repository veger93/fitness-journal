package com.vegas.user.exception;

/**
 * Refresh-токена нет, он просрочен, отозван или чужой -> HTTP 401.
 * Причину наружу не раскрываем: фронту достаточно знать, что нужно войти заново.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Сессия истекла, войдите снова");
    }
}
