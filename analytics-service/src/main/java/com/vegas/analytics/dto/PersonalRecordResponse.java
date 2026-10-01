package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Запись в ленте "Личные рекорды": новый максимум расчётного 1ПМ по упражнению.
 * weightKg × reps — подход, которым рекорд поставлен; deltaKg — на сколько вырос 1ПМ.
 */
public record PersonalRecordResponse(
        UUID exerciseId,
        String exerciseName,
        Instant achievedAt,
        BigDecimal weightKg,
        Integer reps,
        BigDecimal e1rmKg,
        BigDecimal previousE1rmKg,
        BigDecimal deltaKg
) {
}
