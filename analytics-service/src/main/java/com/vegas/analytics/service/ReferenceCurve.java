package com.vegas.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Эталонная кривая: каким был бы 1ПМ, если бы рос с ожидаемым для уровня темпом.
 * Сложный процент от стартовой точки: 1ПМ(t) = 1ПМ(старт) × (1 + темп)^(месяцев с начала).
 * На графике — пунктир рядом с реальной линией.
 */
public final class ReferenceCurve {

    /** Средняя длина месяца в днях (365.25 / 12). */
    private static final double DAYS_IN_MONTH = 30.4375;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ReferenceCurve() {
    }

    public static BigDecimal expected(BigDecimal startE1rm, Instant startAt, Instant at, BigDecimal monthlyGainPercent) {
        double months = Duration.between(startAt, at).toHours() / 24.0 / DAYS_IN_MONTH;
        double rate = monthlyGainPercent.doubleValue() / 100.0;
        double factor = Math.pow(1 + rate, Math.max(0, months));
        return startE1rm.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }

    /** На сколько % факт выше (+) или ниже (−) эталона. Один знак после запятой. */
    public static BigDecimal gapPercent(BigDecimal actual, BigDecimal expected) {
        return actual.subtract(expected).multiply(HUNDRED).divide(expected, 1, RoundingMode.HALF_UP);
    }
}
