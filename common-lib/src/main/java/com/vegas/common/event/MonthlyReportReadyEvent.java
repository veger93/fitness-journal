package com.vegas.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Отчёт за месяц готов. month — "2026-09"; цифры — для текста письма. */
public record MonthlyReportReadyEvent(
        UUID userId,
        String month,
        BigDecimal strengthChangePercent,
        BigDecimal tonnageKg,
        BigDecimal tonnageChangePercent,
        int workouts,
        int records
) {
}
