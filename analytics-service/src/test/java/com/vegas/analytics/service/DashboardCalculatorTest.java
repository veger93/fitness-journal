package com.vegas.analytics.service;

import com.vegas.analytics.dto.DashboardResponse;
import com.vegas.analytics.dto.ExerciseStatus;
import com.vegas.analytics.dto.ExerciseSummaryResponse;
import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.dto.RecordItemResponse;
import com.vegas.analytics.dto.RecordType;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final UUID SQUAT = UUID.randomUUID();
    private static final UUID BENCH = UUID.randomUUID();
    private static final UUID DEADLIFT = UUID.randomUUID();
    private static final UUID CURL = UUID.randomUUID();

    @Test
    void noWorkouts_isEmpty() {
        DashboardResponse dashboard = DashboardCalculator.build(List.of(), List.of(), NOW);

        assertThat(dashboard.empty()).isTrue();
        assertThat(dashboard.month()).isEqualTo("2026-10");
        assertThat(dashboard.exercises()).isEmpty();
        assertThat(dashboard.focusExerciseId()).isNull();
    }

    @Test
    void weekTonnage_comparedWithPreviousWeek() {
        List<ExercisePerformance> history = sorted(List.of(
                perf(CURL, "Сгибания", 10, null, "1000"),
                perf(CURL, "Сгибания", 5, null, "500"),
                perf(CURL, "Сгибания", 2, null, "1000")));

        DashboardResponse dashboard = DashboardCalculator.build(history, List.of(), NOW);

        assertThat(dashboard.weekTonnageKg()).isEqualByComparingTo("1500");
        assertThat(dashboard.previousWeekTonnageKg()).isEqualByComparingTo("1000");
        assertThat(dashboard.weekTonnageChangePercent()).isEqualByComparingTo("50");
    }

    @Test
    void workoutsThisMonth_countsDistinctWorkouts() {
        UUID workout = UUID.randomUUID();
        List<ExercisePerformance> history = sorted(List.of(
                perf(CURL, "Сгибания", 25, null, "100"),          // 25 сентября — прошлый месяц
                perfIn(workout, BENCH, "Жим", 5, "100", "100"),    // одна тренировка,
                perfIn(workout, CURL, "Сгибания", 5, null, "100"), // два упражнения
                perf(CURL, "Сгибания", 2, null, "100")));

        assertThat(DashboardCalculator.build(history, List.of(), NOW).workoutsThisMonth()).isEqualTo(2);
    }

    @Test
    void exercises_problemsFirst_thenRecords_thenRest() {
        List<ExercisePerformance> history = new ArrayList<>();
        // присед: объём резко вырос, сила упала, плато -> высокий риск
        history.add(perf(SQUAT, "Приседания", 25, "110", "1000"));
        history.add(perf(SQUAT, "Приседания", 18, "110", "1000"));
        history.add(perf(SQUAT, "Приседания", 11, "108", "1000"));
        history.add(perf(SQUAT, "Приседания", 3, "100", "2500"));
        // жим: максимум 30 дней назад, потом 3 тренировки без роста -> плато
        history.add(perf(BENCH, "Жим", 35, "95", "1000"));
        history.add(perf(BENCH, "Жим", 30, "100", "1000"));
        history.add(perf(BENCH, "Жим", 20, "99", "1000"));
        history.add(perf(BENCH, "Жим", 10, "98", "1000"));
        history.add(perf(BENCH, "Жим", 4, "99", "1000"));
        // становая: свежий рекорд
        history.add(perf(DEADLIFT, "Становая", 3, "160", "1500"));
        // сгибания: ничего особенного, но самые свежие
        history.add(perf(CURL, "Сгибания", 1, "40", "300"));
        List<PersonalRecordResponse> records = List.of(record(DEADLIFT, 3, "5"));

        DashboardResponse dashboard = DashboardCalculator.build(sorted(history), records, NOW);

        assertThat(dashboard.exercises()).extracting(ExerciseSummaryResponse::status).containsExactly(
                ExerciseStatus.OVERLOAD, ExerciseStatus.PLATEAU, ExerciseStatus.RECORD, ExerciseStatus.NORMAL);
        assertThat(dashboard.exercises()).extracting(ExerciseSummaryResponse::exerciseId)
                .containsExactly(SQUAT, BENCH, DEADLIFT, CURL);
        assertThat(dashboard.exercises().get(1).note()).isEqualTo("30 дн. без нового максимума");
        assertThat(dashboard.exercises().get(2).note()).isEqualTo("+5 кг к прошлому лучшему");
        assertThat(dashboard.exercises().get(3).note()).isNull();
        // большой график — по первому упражнению, требующему внимания
        assertThat(dashboard.focusExerciseId()).isEqualTo(SQUAT);
    }

    @Test
    void oldRecord_isNotFresh() {
        List<ExercisePerformance> history = List.of(perf(DEADLIFT, "Становая", 20, "160", "1500"));

        DashboardResponse dashboard = DashboardCalculator.build(history, List.of(record(DEADLIFT, 20, "5")), NOW);

        assertThat(dashboard.exercises().get(0).status()).isEqualTo(ExerciseStatus.NORMAL);
    }

    @Test
    void lastSetAndCurrentStrength_fromLatestWorkout() {
        List<ExercisePerformance> history = sorted(List.of(
                perf(BENCH, "Жим", 10, "90", "1000"),
                perf(BENCH, "Жим", 2, "95", "1000")));

        ExerciseSummaryResponse bench = DashboardCalculator.build(history, List.of(), NOW).exercises().get(0);

        assertThat(bench.currentE1rmKg()).isEqualByComparingTo("95");
        assertThat(bench.trend()).extracting(BigDecimal::intValue).containsExactly(90, 95);
        assertThat(bench.lastWeightKg()).isEqualByComparingTo("80");
        assertThat(bench.lastReps()).isEqualTo(5);
        assertThat(bench.lastPerformedAt()).isEqualTo(NOW.minus(Duration.ofDays(2)));
    }

    @Test
    void trend_keepsLastEightValuesInOrder() {
        List<ExercisePerformance> history = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            history.add(perf(BENCH, "Жим", 40 - i * 3, String.valueOf(80 + i), "1000"));
        }

        ExerciseSummaryResponse bench = DashboardCalculator.build(history, List.of(), NOW).exercises().get(0);

        assertThat(bench.trend()).extracting(BigDecimal::intValue).containsExactly(82, 83, 84, 85, 86, 87, 88, 89);
    }

    @Test
    void run_showsDistanceAndTime_bodyweight_showsReps() {
        ExercisePerformance run = ExercisePerformance.builder()
                .userId(UUID.randomUUID()).workoutId(UUID.randomUUID()).exerciseId(CURL).exerciseName("Бег")
                .bodyPart(BodyPart.LEGS).trackingType("DISTANCE").performedAt(NOW.minus(Duration.ofDays(1)))
                .workingSets(1).volumeKg(BigDecimal.ZERO).bestDistanceM(5000).bestDurationSec(1600)
                .build();
        ExercisePerformance pullUps = ExercisePerformance.builder()
                .userId(UUID.randomUUID()).workoutId(UUID.randomUUID()).exerciseId(BENCH).exerciseName("Подтягивания")
                .bodyPart(BodyPart.BACK).trackingType("WEIGHT_REPS").performedAt(NOW.minus(Duration.ofDays(2)))
                .workingSets(3).volumeKg(BigDecimal.ZERO).bestReps(12)
                .build();

        List<ExerciseSummaryResponse> exercises = DashboardCalculator.build(List.of(pullUps, run), List.of(), NOW).exercises();

        assertThat(exercises.get(0).trackingType()).isEqualTo("DISTANCE");
        assertThat(exercises.get(0).lastDistanceM()).isEqualTo(5000);
        assertThat(exercises.get(0).lastDurationSec()).isEqualTo(1600);
        assertThat(exercises.get(1).lastWeightKg()).isNull();
        assertThat(exercises.get(1).lastReps()).isEqualTo(12);
    }

    @Test
    void run_kilometreSpike_isOverload_speedTrend() {
        List<ExercisePerformance> runs = List.of(
                RunningCalculatorTest.run(25, 5000, 1800, null), RunningCalculatorTest.run(18, 5000, 1800, null),
                RunningCalculatorTest.run(10, 7000, 2520, null), RunningCalculatorTest.run(3, 8000, 2700, null));

        ExerciseSummaryResponse run = DashboardCalculator.build(runs, List.of(), RunningCalculatorTest.NOW_FOR_DASHBOARD)
                .exercises().get(0);

        assertThat(run.status()).isEqualTo(ExerciseStatus.OVERLOAD);
        assertThat(run.note()).isEqualTo("Километраж за 2 недели +50%");
        assertThat(run.lastDistanceM()).isEqualTo(8000);
        assertThat(run.trend()).extracting(BigDecimal::doubleValue).containsExactly(10.0, 10.0, 10.0, 10.7);
    }

    // ---------- helpers ----------

    private static List<ExercisePerformance> sorted(List<ExercisePerformance> list) {
        return list.stream().sorted(Comparator.comparing(ExercisePerformance::getPerformedAt)).toList();
    }

    private static ExercisePerformance perf(UUID exerciseId, String name, int daysAgo, String e1rm, String volume) {
        return perfIn(UUID.randomUUID(), exerciseId, name, daysAgo, e1rm, volume);
    }

    /** e1rm = null — упражнение без расчётного 1ПМ. Подход для карточки всегда "80 × 5" — значение не важно. */
    private static ExercisePerformance perfIn(UUID workoutId, UUID exerciseId, String name, int daysAgo, String e1rm, String volume) {
        return ExercisePerformance.builder()
                .userId(UUID.randomUUID())
                .workoutId(workoutId)
                .exerciseId(exerciseId)
                .exerciseName(name)
                .bodyPart(BodyPart.CHEST)
                .trackingType("WEIGHT_REPS")
                .performedAt(NOW.minus(Duration.ofDays(daysAgo)))
                .workingSets(3)
                .volumeKg(new BigDecimal(volume))
                .bestE1rmKg(e1rm == null ? null : new BigDecimal(e1rm))
                .maxWeightKg(new BigDecimal("80"))
                .maxWeightReps(5)
                .build();
    }

    private static PersonalRecordResponse record(UUID exerciseId, int daysAgo, String deltaKg) {
        return new PersonalRecordResponse(UUID.randomUUID(), exerciseId, "Становая", NOW.minus(Duration.ofDays(daysAgo)),
                new BigDecimal("150"), 3, List.of(new RecordItemResponse(RecordType.WEIGHT, new BigDecimal("150"), 3,
                new BigDecimal("150"), new BigDecimal("145"), new BigDecimal(deltaKg), null)));
    }
}
