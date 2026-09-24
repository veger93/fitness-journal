package com.vegas.common.exception;

/** Нарушено бизнес-правило (например, email уже занят) → HTTP 409/422. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
