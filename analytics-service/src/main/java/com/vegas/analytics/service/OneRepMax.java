package com.vegas.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Расчётный одноповторный максимум (1ПМ, e1RM) — какой вес человек поднял бы на 1 раз.
 * Формула Эпли: 1ПМ = вес × (1 + повторы / 30).
 * Точна примерно до 10 повторов; на 15+ повторах завышает — для силовых упражнений годится.
 * Источники: docs/references.md [1], [2].
 */
public final class OneRepMax {

    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);

    private OneRepMax() {
    }

    public static BigDecimal epley(BigDecimal weightKg, int reps) {
        if (reps < 1) {
            throw new IllegalArgumentException("reps must be >= 1");
        }
        if (reps == 1) {
            return weightKg.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(reps).divide(THIRTY, 10, RoundingMode.HALF_UP));
        return weightKg.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
