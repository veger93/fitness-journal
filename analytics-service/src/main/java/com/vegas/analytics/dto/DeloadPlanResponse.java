package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * План разгрузочной недели (экран "План разгрузки").
 * workingWeightKg = intensityPercent% от текущего 1ПМ, округлено до 2.5 кг (null у упражнений без веса).
 * sets — подходов за тренировку (≈ половина обычного), reps — повторов в подходе.
 */
public record DeloadPlanResponse(
        UUID exerciseId,
        String exerciseName,
        String reason,
        int durationDays,
        int intensityPercent,
        BigDecimal currentE1rmKg,
        BigDecimal workingWeightKg,
        int sets,
        int reps,
        List<String> notes
) {
}
