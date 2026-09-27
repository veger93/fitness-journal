package com.vegas.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Тело запроса POST /api/auth/register.
 * Аннотации проверяются, когда в контроллере стоит @Valid. Ошибка -> 400 с описанием полей.
 */
public record RegisterRequest(

        @NotBlank(message = "email обязателен")
        @Email(message = "некорректный email")
        @Size(max = 255, message = "email слишком длинный")
        String email,

        // BCrypt учитывает только первые 72 байта пароля — длиннее принимать нет смысла
        @NotBlank(message = "пароль обязателен")
        @Size(min = 8, max = 72, message = "пароль должен быть от 8 до 72 символов")
        String password,

        @Size(max = 100, message = "имя не длиннее 100 символов")
        String displayName
) {
}
