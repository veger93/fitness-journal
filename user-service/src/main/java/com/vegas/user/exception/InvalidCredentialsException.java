package com.vegas.user.exception;

/**
 * Неверный email или пароль -> HTTP 401.
 * Сообщение ОДИНАКОВОЕ для "нет такого email" и "неверный пароль":
 * иначе по ответу можно перебором выяснить, какие email зарегистрированы.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Неверный email или пароль");
    }
}
