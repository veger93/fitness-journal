package com.vegas.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/users/me/clients/{clientId} — тренер правит карточку клиента.
 * Имя меняется только у ведомого клиента: у самостоятельного это его аккаунт, не тренера.
 */
public record UpdateClientRequest(

        @NotBlank(message = "имя обязательно")
        @Size(max = 100, message = "имя до 100 символов")
        String displayName,

        @Size(max = 30, message = "телефон до 30 символов")
        @Pattern(regexp = "[0-9+()\\- ]*", message = "телефон: цифры, пробелы, + ( ) -")
        String phone
) {
}
