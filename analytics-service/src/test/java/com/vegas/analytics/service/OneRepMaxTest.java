package com.vegas.analytics.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OneRepMaxTest {

    /** @ParameterizedTest: один тест — много наборов данных из таблицы. */
    @ParameterizedTest
    @CsvSource({
            "100, 1, 100.00",   // 1 повтор = сам вес
            "100, 5, 116.67",   // 100 × (1 + 5/30)
            "80,  8, 101.33",   // 80 × (1 + 8/30)
            "60, 10,  80.00"    // 60 × (1 + 10/30)
    })
    void epley(String weight, int reps, String expected) {
        assertThat(OneRepMax.epley(new BigDecimal(weight), reps)).isEqualByComparingTo(expected);
    }

    @Test
    void zeroReps_isRejected() {
        assertThatThrownBy(() -> OneRepMax.epley(BigDecimal.TEN, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
