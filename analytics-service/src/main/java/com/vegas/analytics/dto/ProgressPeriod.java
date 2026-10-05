package com.vegas.analytics.dto;

import java.time.Instant;
import java.time.ZoneOffset;

/** Период графика (выпадающий список "3 месяца" на детальной странице упражнения). */
public enum ProgressPeriod {
    M1(1),
    M3(3),
    M6(6),
    Y1(12),
    ALL(0);

    private final int months;

    ProgressPeriod(int months) {
        this.months = months;
    }

    public Instant startFrom(Instant now) {
        return this == ALL ? Instant.EPOCH : now.atOffset(ZoneOffset.UTC).minusMonths(months).toInstant();
    }
}
