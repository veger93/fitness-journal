package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Карточка в ленте "Личные рекорды": одно упражнение в одной тренировке.
 * Если побито несколько видов рекорда, они не дублируют карточку, а лежат в records (бейджи).
 * weightKg × reps — подход для заголовка карточки ("145 кг × 5"): подход рекорда веса, если он есть.
 */
public record PersonalRecordResponse(
        UUID exerciseId,
        String exerciseName,
        Instant achievedAt,
        BigDecimal weightKg,
        Integer reps,
        List<RecordItemResponse> records
) {
}
