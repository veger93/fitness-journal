package com.vegas.user.dto;

import com.vegas.common.model.Gender;
import com.vegas.user.entity.ExperienceLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * PUT /api/users/me/profile — экран онбординга и "Физические данные" в настройках.
 * PUT = полная замена: фронт всегда присылает все поля.
 * Возраст (через birthYear) проверяется в сервисе: границы зависят от текущего года.
 */
public record UpdateProfileRequest(

        @NotNull(message = "пол обязателен")
        Gender gender,

        @NotNull(message = "год рождения обязателен")
        Integer birthYear,

        @NotNull(message = "рост обязателен")
        @Min(value = 100, message = "рост от 100 см")
        @Max(value = 250, message = "рост до 250 см")
        Integer heightCm,

        @NotNull(message = "опыт тренировок обязателен")
        ExperienceLevel experienceLevel,

        @NotNull(message = "вес обязателен")
        @DecimalMin(value = "20.01", message = "вес больше 20 кг")
        @DecimalMax(value = "399.99", message = "вес меньше 400 кг")
        @Digits(integer = 3, fraction = 2, message = "вес: не больше 2 знаков после запятой")
        BigDecimal weightKg
) {
}
