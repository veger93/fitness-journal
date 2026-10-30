package com.vegas.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/auth/claim/{token} — клиент получает аккаунт от тренера.
 * Правила email и пароля — те же, что при обычной регистрации (RegisterRequest).
 */
public record ClaimRequest(

        @NotBlank(message = "email обязателен")
        @Email(message = "некорректный email")
        @Size(max = 255, message = "email слишком длинный")
        String email,

        @NotBlank(message = "пароль обязателен")
        @Size(min = 8, max = 72, message = "пароль должен быть от 8 до 72 символов")
        String password,

        /** true — продолжаю с тренером (он видит и записывает), false — дальше сам */
        @NotNull(message = "выберите: с тренером или самостоятельно")
        Boolean keepTrainer,

        /** согласие на обработку данных: без него аккаунт не передаём */
        @AssertTrue(message = "нужно согласие на обработку данных")
        boolean consent
) {
}
