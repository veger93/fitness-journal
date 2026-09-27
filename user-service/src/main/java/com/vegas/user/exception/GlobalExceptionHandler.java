package com.vegas.user.exception;

import com.vegas.common.dto.ErrorResponse;
import com.vegas.common.web.CommonExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }
}
