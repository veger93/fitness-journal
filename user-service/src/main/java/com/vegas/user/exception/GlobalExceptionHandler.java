package com.vegas.user.exception;

import com.vegas.common.dto.ErrorResponse;
import com.vegas.common.web.CommonExceptionHandler;
import com.vegas.user.security.RefreshTokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Обработка ошибок user-service.
 * Общие случаи (400/403/404/409/500) — в CommonExceptionHandler из common-lib,
 * здесь только то, что специфично для этого сервиса.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends CommonExceptionHandler {

    private final RefreshTokenCookie refreshTokenCookie;

    public GlobalExceptionHandler(RefreshTokenCookie refreshTokenCookie) {
        this.refreshTokenCookie = refreshTokenCookie;
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    /** Плохой refresh-токен: 401 и сразу удаляем cookie, чтобы браузер не слал мёртвый токен снова и снова */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
                .body(build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request).getBody());
    }
}
