package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Строка таблицы полного отчёта: "Жим лёжа (1ПМ)  90 -> 95 кг  +5%".
 * before — лучший 1ПМ прошлого месяца (если упражнения тогда не было — первый результат этого месяца),
 * after — лучший 1ПМ этого месяца.
 */
public record ExerciseMonthResponse(
        UUID exerciseId,
        String exerciseName,
        BigDecimal beforeE1rmKg,
        BigDecimal afterE1rmKg,
        BigDecimal changePercent,
        int sessions
) {
}
