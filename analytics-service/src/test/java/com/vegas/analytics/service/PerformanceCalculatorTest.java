package com.vegas.analytics.service;

import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutCompletedEvent.ExercisePerformed;
import com.vegas.common.event.WorkoutCompletedEvent.SetPerformed;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceCalculatorTest {

    private static final Instant COMPLETED_AT = Instant.parse("2026-10-01T11:00:00Z");

    @Test
    void weightedSets_bestSetByE1rm_volumeWithoutWarmups() {
        ExercisePerformed bench = exercise("WEIGHT_REPS",
                set("40", 10, true),    // разминка: не считается
                set("80", 8, false),    // e1RM 101.33
                set("85", 5, false),    // e1RM 99.17 — вес больше, но 1ПМ ниже
                set("80", 6, false));   // e1RM 96.00

        ExercisePerformance result = PerformanceCalculator.calculate(event(bench), bench).orElseThrow();

        assertThat(result.getWorkingSets()).isEqualTo(3);
        assertThat(result.getTotalReps()).isEqualTo(19);
        assertThat(result.getVolumeKg()).isEqualByComparingTo("1545");     // 640 + 425 + 480
        assertThat(result.getBestWeightKg()).isEqualByComparingTo("80");
        assertThat(result.getBestReps()).isEqualTo(8);
        assertThat(result.getBestE1rmKg()).isEqualByComparingTo("101.33");
        assertThat(result.getPerformedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void bodyweightSets_noE1rm_bestRepsIsMax() {
        ExercisePerformed pullUps = exercise("WEIGHT_REPS",
                new SetPerformed(null, 10, null, null, null, false),
                new SetPerformed(null, 12, null, null, null, false));

        ExercisePerformance result = PerformanceCalculator.calculate(event(pullUps), pullUps).orElseThrow();

        assertThat(result.getBestE1rmKg()).isNull();
        assertThat(result.getBestReps()).isEqualTo(12);
        assertThat(result.getVolumeKg()).isEqualByComparingTo("0");
    }

    @Test
    void timeSets_bestDuration() {
        ExercisePerformed plank = exercise("TIME",
                new SetPerformed(null, null, 60, null, null, false),
                new SetPerformed(null, null, 90, null, null, false));

        ExercisePerformance result = PerformanceCalculator.calculate(event(plank), plank).orElseThrow();

        assertThat(result.getBestDurationSec()).isEqualTo(90);
        assertThat(result.getTotalReps()).isNull();
    }

    @Test
    void onlyWarmups_givesNothing() {
        ExercisePerformed bench = exercise("WEIGHT_REPS", set("40", 10, true));

        Optional<ExercisePerformance> result = PerformanceCalculator.calculate(event(bench), bench);

        assertThat(result).isEmpty();
    }

    private static WorkoutCompletedEvent event(ExercisePerformed exercise) {
        return new WorkoutCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), COMPLETED_AT, List.of(exercise));
    }

    private static ExercisePerformed exercise(String trackingType, SetPerformed... sets) {
        return new ExercisePerformed(UUID.randomUUID(), "Упражнение", BodyPart.CHEST, trackingType, List.of(sets));
    }

    private static SetPerformed set(String weight, int reps, boolean warmup) {
        return new SetPerformed(new BigDecimal(weight), reps, null, null, null, warmup);
    }
}
