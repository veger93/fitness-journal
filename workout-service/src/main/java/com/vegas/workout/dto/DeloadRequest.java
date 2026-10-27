package com.vegas.workout.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * PUT /api/exercises/{id}/deload — начать разгрузку. Параметры берутся из плана разгрузки,
 * который считает analytics-service (фронт показывает план и присылает его сюда).
 * workingWeightKg = null — упражнение без веса.
 */
public record DeloadRequest(
        @DecimalMin(value = "0.5", message = "вес больше 0") @DecimalMax(value = "1000", message = "вес до 1000 кг")
        BigDecimal workingWeightKg,

        @NotNull @Min(1) @Max(10)
        Integer sets,

        @NotNull @Min(1) @Max(50)
        Integer reps,

        // неделя — обычная длина разгрузки; больше двух — это уже не разгрузка, а перерыв
        @NotNull @Min(value = 1, message = "от 1 дня") @Max(value = 14, message = "не больше 14 дней")
        Integer days
) {
}
