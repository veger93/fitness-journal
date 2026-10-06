package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Точка графика: одна тренировка с этим упражнением.
 * referenceE1rmKg — значение эталонной кривой на эту дату (пунктир на графике).
 */
public record ProgressPointResponse(
        Instant date,
        BigDecimal e1rmKg,
        BigDecimal bestWeightKg,
        Integer bestReps,
        BigDecimal volumeKg,
        BigDecimal referenceE1rmKg
) {
}
