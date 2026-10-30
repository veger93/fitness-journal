package com.vegas.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** POST /api/auth/password/reset — код из письма + новый пароль (правила пароля как при регистрации) */
public record ResetPasswordRequest(
        @NotBlank(message = "email обязателен")
        @Email(message = "некорректный email")
        String email,

        @NotBlank(message = "введите код")
        @Pattern(regexp = "\\d{6}", message = "код — 6 цифр")
        String code,

        @NotBlank(message = "пароль обязателен")
        @Size(min = 8, max = 72, message = "пароль должен быть от 8 до 72 символов")
        String newPassword
) {
}
