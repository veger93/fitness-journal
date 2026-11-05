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

    @Test
    void assisted_counterweightBecomesLoad_bodyWeightMinusAssist() {
        // гравитрон: вес тела 80, противовес 20 × 8 и 15 × 6 -> нагрузка 60 × 8 и 65 × 6
        ExercisePerformed gravitron = new ExercisePerformed(UUID.randomUUID(), "Подтягивания в гравитроне",
                BodyPart.BACK, "WEIGHT_REPS", "VERTICAL_PULL", true, List.of(set("20", 8, false), set("15", 6, false)));

        ExercisePerformance result = PerformanceCalculator.calculate(event(gravitron), gravitron, new BigDecimal("80"))
                .orElseThrow();

        assertThat(result.getMaxWeightKg()).isEqualByComparingTo("65"); // меньше противовес = тяжелее
        assertThat(result.getVolumeKg()).isEqualByComparingTo("870");    // 60×8 + 65×6
        assertThat(result.getBestE1rmKg()).isEqualByComparingTo(OneRepMax.epley(new BigDecimal("60"), 8)
                .max(OneRepMax.epley(new BigDecimal("65"), 6)));
    }

    @Test
    void assisted_noBodyWeight_onlyReps() {
        ExercisePerformed gravitron = new ExercisePerformed(UUID.randomUUID(), "Подтягивания в гравитроне",
                BodyPart.BACK, "WEIGHT_REPS", "VERTICAL_PULL", true, List.of(set("20", 8, false)));

        ExercisePerformance result = PerformanceCalculator.calculate(event(gravitron), gravitron, null).orElseThrow();

        // нагрузку не знаем — как подтягивания без веса: только повторы, без 1ПМ и "рекордов веса"
        assertThat(result.getBestE1rmKg()).isNull();
        assertThat(result.getMaxWeightKg()).isNull();
        assertThat(result.getBestReps()).isEqualTo(8);
    }

    private static final Instant COMPLETED_AT = Instant.parse("2026-10-01T11:00:00Z");

    @Test
    void weightedSets_bestSetByE1rm_volumeWithoutWarmups() {
        ExercisePerformed bench = exercise("WEIGHT_REPS",
                set("40", 10, true),    // разминка: не считается
                set("80", 8, false),    // e1RM 101.33
                set("85", 5, false),    // e1RM 99.17 — вес больше, но 1ПМ ниже
                set("80", 6, false));   // e1RM 96.00

        ExercisePerformance result = PerformanceCalculator.calculate(event(bench), bench, null).orElseThrow();

        assertThat(result.getWorkingSets()).isEqualTo(3);
        assertThat(result.getTotalReps()).isEqualTo(19);
        assertThat(result.getVolumeKg()).isEqualByComparingTo("1545");     // 640 + 425 + 480
        assertThat(result.getBestWeightKg()).isEqualByComparingTo("80");
        assertThat(result.getBestReps()).isEqualTo(8);
        assertThat(result.getBestE1rmKg()).isEqualByComparingTo("101.33");
        // самый тяжёлый подход — другой: 85×5
        assertThat(result.getMaxWeightKg()).isEqualByComparingTo("85");
        assertThat(result.getMaxWeightReps()).isEqualTo(5);
        assertThat(result.getAvgRpe()).isNull(); // RPE не указывали
        assertThat(result.getPerformedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void bodyweightSets_noE1rm_bestRepsIsMax() {
        ExercisePerformed pullUps = exercise("WEIGHT_REPS",
                new SetPerformed(null, 10, null, null, null, false),
                new SetPerformed(null, 12, null, null, null, false));

        ExercisePerformance result = PerformanceCalculator.calculate(event(pullUps), pullUps, null).orElseThrow();

        assertThat(result.getBestE1rmKg()).isNull();
        assertThat(result.getMaxWeightKg()).isNull();
        assertThat(result.getBestReps()).isEqualTo(12);
        assertThat(result.getVolumeKg()).isEqualByComparingTo("0");
    }

    @Test
    void timeSets_bestDuration() {
        ExercisePerformed plank = exercise("TIME",
                new SetPerformed(null, null, 60, null, null, false),
                new SetPerformed(null, null, 90, null, null, false));

        ExercisePerformance result = PerformanceCalculator.calculate(event(plank), plank, null).orElseThrow();

        assertThat(result.getBestDurationSec()).isEqualTo(90);
        assertThat(result.getTotalReps()).isNull();
    }

    @Test
    void run_totalsAndBestPaceOnlyFromKilometrePlus() {
        ExercisePerformed intervals = exercise("DISTANCE",
                new SetPerformed(null, null, 600, 1000, null, true),     // разминка — не считается
                new SetPerformed(null, null, 300, 1000, null, false),    // 5:00 /км
                new SetPerformed(null, null, 60, 400, null, false),      // 2:30 /км, но короче километра
                new SetPerformed(null, null, 290, 1000, null, false));   // 4:50 /км — лучший

        ExercisePerformance result = PerformanceCalculator.calculate(event(intervals), intervals, null).orElseThrow();

        assertThat(result.getTotalDistanceM()).isEqualTo(2400);
        assertThat(result.getTotalDurationSec()).isEqualTo(650);
        assertThat(result.getBestPaceSecPerKm()).isEqualByComparingTo("290");
    }

    @Test
    void averageRpe_onlyOverSetsWhereItIsSet() {
        ExercisePerformed squat = exercise("WEIGHT_REPS",
                new SetPerformed(new BigDecimal("100"), 5, null, null, new BigDecimal("8"), false),
                new SetPerformed(new BigDecimal("100"), 5, null, null, new BigDecimal("9.5"), false),
                new SetPerformed(new BigDecimal("100"), 5, null, null, null, false));

        ExercisePerformance result = PerformanceCalculator.calculate(event(squat), squat, null).orElseThrow();

        assertThat(result.getAvgRpe()).isEqualByComparingTo("8.8"); // (8 + 9.5) / 2 = 8.75 -> 8.8
    }

    @Test
    void onlyWarmups_givesNothing() {
        ExercisePerformed bench = exercise("WEIGHT_REPS", set("40", 10, true));

        Optional<ExercisePerformance> result = PerformanceCalculator.calculate(event(bench), bench, null);

        assertThat(result).isEmpty();
    }

    private static WorkoutCompletedEvent event(ExercisePerformed exercise) {
        return new WorkoutCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), COMPLETED_AT, List.of(exercise));
    }

    private static ExercisePerformed exercise(String trackingType, SetPerformed... sets) {
        return new ExercisePerformed(UUID.randomUUID(), "Упражнение", BodyPart.CHEST, trackingType, null, null, List.of(sets));
    }

    private static SetPerformed set(String weight, int reps, boolean warmup) {
        return new SetPerformed(new BigDecimal(weight), reps, null, null, null, warmup);
    }
}
