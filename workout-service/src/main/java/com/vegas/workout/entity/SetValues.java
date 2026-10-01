package com.vegas.workout.entity;

import java.math.BigDecimal;

/**
 * Значения подхода. Отдельный класс, чтобы сущность не зависела от DTO из API:
 * сервис превращает запрос в SetValues, сущность работает только с ним.
 */
public record SetValues(
        BigDecimal weightKg,
        Integer reps,
        Integer durationSec,
        Integer distanceM,
        BigDecimal rpe,
        boolean warmup
) {
}
