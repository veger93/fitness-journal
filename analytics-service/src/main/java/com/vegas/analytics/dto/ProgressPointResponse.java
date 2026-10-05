package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Точка графика: одна тренировка с этим упражнением. */
public record ProgressPointResponse(
        Instant date,
        BigDecimal e1rmKg,
        BigDecimal bestWeightKg,
        Integer bestReps,
        BigDecimal volumeKg
) {
}
