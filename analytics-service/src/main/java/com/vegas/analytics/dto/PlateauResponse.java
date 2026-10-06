package com.vegas.analytics.dto;

import java.time.Instant;

/**
 * Плато: расчётный 1ПМ давно не растёт, хотя тренировки с упражнением были.
 * lastProgressAt — когда 1ПМ последний раз обновлял максимум (зона "плато · 21 день" на графике начинается отсюда).
 */
public record PlateauResponse(
        boolean detected,
        Instant lastProgressAt,
        long daysSinceProgress,
        int sessionsSinceProgress
) {
}
