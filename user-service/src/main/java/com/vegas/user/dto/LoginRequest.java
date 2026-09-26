package com.vegas.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Тело запроса POST /api/auth/login. */
public record LoginRequest(

        @NotBlank(message = "email обязателен")
        String email,

        @NotBlank(message = "пароль обязателен")
        String password
) {
}
