package com.vegas.common.exception;

/**
 * Запрос синтаксически верный, но данные не проходят бизнес-проверку -> HTTP 400.
 * Например: возраст 3 года или дата замера веса в будущем.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
