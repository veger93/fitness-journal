package com.vegas.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT /api/users/me/email — исправить email (ошибся при регистрации). Пароль — чтобы чужой не увёл аккаунт с открытого телефона */
public record ChangeEmailRequest(
        @NotBlank(message = "email обязателен")
        @Email(message = "некорректный email")
        @Size(max = 255, message = "email слишком длинный")
        String email,

        @NotBlank(message = "введите пароль")
        String password
) {
}
