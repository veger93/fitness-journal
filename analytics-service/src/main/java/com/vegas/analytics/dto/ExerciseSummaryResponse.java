package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Карточка упражнения на главной: "Приседания · 110 кг × 5 · перегрузка".
 * lastWeightKg × lastReps — самый тяжёлый подход последней тренировки (null у упражнений без веса);
 * currentE1rmKg — последний расчётный 1ПМ (null, если не считается);
 * note — короткое пояснение к статусу простыми словами (null у NORMAL);
 * trend — последние значения 1ПМ по тренировкам, от старых к новым (мини-график на главной; пусто у упражнений без веса).
 */
public record ExerciseSummaryResponse(
        UUID exerciseId,
        String exerciseName,
        Instant lastPerformedAt,
        BigDecimal lastWeightKg,
        Integer lastReps,
        BigDecimal currentE1rmKg,
        ExerciseStatus status,
        RiskLevel riskLevel,
        String note,
        List<BigDecimal> trend
) {
}
