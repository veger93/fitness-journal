package com.vegas.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** POST /api/auth/password/forgot — прислать код для смены пароля */
public record ForgotPasswordRequest(
        @NotBlank(message = "email обязателен")
        @Email(message = "некорректный email")
        String email
) {
}
