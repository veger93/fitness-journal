package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Детальная страница упражнения: график + метрики.
 * currentE1rmKg — "1ПМ 95 кг" (последнее значение);
 * changePercent — рост 1ПМ за период;
 * weeklyVolumeKg — "Объём/нед": тоннаж по упражнению за последние 7 дней.
 * Нет данных за период -> points пустой, метрики null (на фронте — empty state).
 */
public record ExerciseProgressResponse(
        UUID exerciseId,
        String exerciseName,
        ProgressPeriod period,
        BigDecimal currentE1rmKg,
        BigDecimal changePercent,
        BigDecimal weeklyVolumeKg,
        List<ProgressPointResponse> points
) {
}
