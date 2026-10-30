package com.vegas.user.dto;

import com.vegas.user.entity.ExperienceLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * POST /api/users/me/clients — тренер заводит клиента.
 * Обязательно только имя: это данные человека, который ещё не давал согласия, — берём минимум.
 * Остальное по желанию (null = не указано); границы те же, что в онбординге.
 */
public record CreateClientRequest(

        @NotBlank(message = "имя обязательно")
        @Size(max = 100, message = "имя до 100 символов")
        String displayName,

        @Size(max = 30, message = "телефон до 30 символов")
        @Pattern(regexp = "[0-9+()\\- ]*", message = "телефон: цифры, пробелы, + ( ) -")
        String phone,

        @Min(value = 100, message = "рост от 100 см")
        @Max(value = 250, message = "рост до 250 см")
        Integer heightCm,

        ExperienceLevel experienceLevel,

        @DecimalMin(value = "20.01", message = "вес больше 20 кг")
        @DecimalMax(value = "399.99", message = "вес меньше 400 кг")
        @Digits(integer = 3, fraction = 2, message = "вес: не больше 2 знаков после запятой")
        BigDecimal weightKg
) {
}
