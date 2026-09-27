package com.vegas.common.exception;

/**
 * Ресурс существует и виден пользователю, но действие запрещено -> HTTP 403.
 * Например: попытка отредактировать системное упражнение из каталога.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
