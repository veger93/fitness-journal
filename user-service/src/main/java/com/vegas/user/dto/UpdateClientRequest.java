package com.vegas.user.dto;

import com.vegas.user.entity.ExperienceLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/users/me/clients/{clientId} — тренер правит карточку клиента.
 * Имя, рост и уровень меняются только у ведомого клиента: у самостоятельного это его аккаунт, не тренера
 * (фронт присылает их без изменений). Рост и уровень — по желанию: null = не указано.
 */
public record UpdateClientRequest(

        @NotBlank(message = "имя обязательно")
        @Size(max = 100, message = "имя до 100 символов")
        String displayName,

        @Size(max = 30, message = "телефон до 30 символов")
        @Pattern(regexp = "[0-9+()\\- ]*", message = "телефон: цифры, пробелы, + ( ) -")
        String phone,

        @Min(value = 100, message = "рост от 100 см")
        @Max(value = 250, message = "рост до 250 см")
        Integer heightCm,

        ExperienceLevel experienceLevel
) {
}
