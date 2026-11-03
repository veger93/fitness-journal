package com.vegas.analytics.service;

import com.vegas.analytics.entity.ExercisePerformance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StrengthCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");

    @Test
    void lightDayAfterHeavy_strengthStaysAtBest() {
        // случай из дневника: 27,5 × 10 (36,7) позавчера, 15 × 10 (20) вчера — сила 36,7, а не 20
        StrengthCalculator.Strength strength = StrengthCalculator.of(List.of(perf(2, "36.7"), perf(1, "20")), NOW);

        assertThat(strength.kg()).isEqualByComparingTo("36.7");
        assertThat(strength.stale()).isFalse();
    }

    @Test
    void bestOlderThan4Weeks_notCounted() {
        StrengthCalculator.Strength strength = StrengthCalculator.of(List.of(perf(35, "100"), perf(10, "90")), NOW);

        assertThat(strength.kg()).isEqualByComparingTo("90");
    }

    @Test
    void notDoneFor6Weeks_strengthAtLastWorkout_markedStale() {
        // последняя тренировка 42 дня назад; окно — 4 недели до неё (60 дней назад входит, 80 — нет)
        StrengthCalculator.Strength strength = StrengthCalculator.of(
                List.of(perf(80, "120"), perf(60, "100"), perf(42, "95")), NOW);

        assertThat(strength.kg()).isEqualByComparingTo("100");
        assertThat(strength.stale()).isTrue();
    }

    @Test
    void noE1rm_null() {
        StrengthCalculator.Strength strength = StrengthCalculator.of(List.of(perf(1, null)), NOW);

        assertThat(strength.kg()).isNull();
        assertThat(strength.stale()).isFalse();
    }

    private static ExercisePerformance perf(int daysAgo, String e1rm) {
        return ExercisePerformance.builder()
                .userId(UUID.randomUUID())
                .workoutId(UUID.randomUUID())
                .exerciseId(UUID.randomUUID())
                .exerciseName("Жим лёжа")
                .trackingType("WEIGHT_REPS")
                .performedAt(NOW.minus(Duration.ofDays(daysAgo)))
                .workingSets(3)
                .volumeKg(BigDecimal.TEN)
                .bestE1rmKg(e1rm == null ? null : new BigDecimal(e1rm))
                .build();
    }
}
