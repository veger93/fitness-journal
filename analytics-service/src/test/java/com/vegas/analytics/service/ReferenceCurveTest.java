package com.vegas.analytics.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceCurveTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void compoundMonthlyGrowth() {
        // 12 месяцев по 1.5% -> 1.015^12 = 1.1956 -> 119.56
        Instant yearLater = START.plus(Duration.ofHours((long) (365.25 * 24)));

        assertThat(ReferenceCurve.expected(new BigDecimal("100"), START, yearLater, new BigDecimal("1.5")))
                .isEqualByComparingTo("119.56");
    }

    @Test
    void atStart_equalsStartValue() {
        assertThat(ReferenceCurve.expected(new BigDecimal("95"), START, START, new BigDecimal("3")))
                .isEqualByComparingTo("95");
    }

    @Test
    void gapPercent_signShowsAheadOrBehind() {
        assertThat(ReferenceCurve.gapPercent(new BigDecimal("94"), new BigDecimal("100"))).isEqualByComparingTo("-6.0");
        assertThat(ReferenceCurve.gapPercent(new BigDecimal("105"), new BigDecimal("100"))).isEqualByComparingTo("5.0");
    }
}
