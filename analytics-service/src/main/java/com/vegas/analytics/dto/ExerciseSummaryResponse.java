package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Карточка упражнения на главной: "Приседания · 110 кг × 5 · перегрузка".
 * trackingType — что записываем: WEIGHT_REPS / TIME / DISTANCE (от этого зависит, как показывать результат);
 * lastWeightKg × lastReps — самый тяжёлый подход последней тренировки; без веса (подтягивания) — только lastReps;
 * lastDistanceM, lastDurationSec — лучшие дистанция и время последней тренировки (бег, планка);
 * currentE1rmKg — последний расчётный 1ПМ (null, если не считается);
 * note — короткое пояснение к статусу простыми словами (null у NORMAL);
 * trend — последние значения 1ПМ по тренировкам, от старых к новым (мини-график на главной; пусто у упражнений без веса).
 */
public record ExerciseSummaryResponse(
        UUID exerciseId,
        String exerciseName,
        String trackingType,
        Instant lastPerformedAt,
        BigDecimal lastWeightKg,
        Integer lastReps,
        Integer lastDistanceM,
        Integer lastDurationSec,
        BigDecimal currentE1rmKg,
        ExerciseStatus status,
        RiskLevel riskLevel,
        String note,
        List<BigDecimal> trend
) {
}
