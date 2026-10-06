package com.vegas.analytics.service;

import com.vegas.analytics.dto.ExerciseMonthResponse;
import com.vegas.analytics.dto.MonthlyReportResponse;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyReportCalculatorTest {

    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);
    private final UUID bench = UUID.randomUUID();
    private final UUID squat = UUID.randomUUID();
    private final UUID pullUps = UUID.randomUUID();

    @Test
    void fullReport_beforeIsBestOfPreviousMonth_afterIsBestOfThisMonth() {
        UUID aug1 = UUID.randomUUID(), aug2 = UUID.randomUUID();
        UUID sep1 = UUID.randomUUID(), sep2 = UUID.randomUUID(), sep3 = UUID.randomUUID();
        List<ExercisePerformance> history = List.of(
                // август
                perf(bench, "Жим лёжа", aug1, "2026-08-05", "88", "2000"),
                perf(bench, "Жим лёжа", aug2, "2026-08-20", "90", "2000"),
                perf(squat, "Присед", aug2, "2026-08-20", "115", "800"),
                // сентябрь
                perf(bench, "Жим лёжа", sep1, "2026-09-03", "92", "2200"),
                perf(bench, "Жим лёжа", sep2, "2026-09-17", "95", "2300"),
                perf(squat, "Присед", sep2, "2026-09-17", "124", "1000"),
                perf(pullUps, "Подтягивания", sep3, "2026-09-25", null, "0"));

        MonthlyReportResponse report = MonthlyReportCalculator.build(SEPTEMBER, history, 3);

        assertThat(report.month()).isEqualTo("2026-09");
        assertThat(report.empty()).isFalse();
        assertThat(report.recordsCount()).isEqualTo(3);

        // строки таблицы: подтягиваний без веса нет; жим (2 тренировки) выше приседа (1)
        assertThat(report.exercises()).extracting(ExerciseMonthResponse::exerciseName).containsExactly("Жим лёжа", "Присед");
        ExerciseMonthResponse benchRow = report.exercises().get(0);
        assertThat(benchRow.beforeE1rmKg()).isEqualByComparingTo("90");   // лучший в августе
        assertThat(benchRow.afterE1rmKg()).isEqualByComparingTo("95");    // лучший в сентябре
        assertThat(benchRow.changePercent()).isEqualByComparingTo("5.6");
        assertThat(benchRow.sessions()).isEqualTo(2);

        // сила в среднем: было (90 + 115) / 2 = 102.5, стало (95 + 124) / 2 = 109.5 -> +6.8%
        assertThat(report.strength().before()).isEqualByComparingTo("102.5");
        assertThat(report.strength().after()).isEqualByComparingTo("109.5");
        assertThat(report.strength().changePercent()).isEqualByComparingTo("6.8");
        assertThat(report.strength().hint()).contains("по 2 упражнениям");

        // тоннаж: август 4800, сентябрь 5500 -> +14.6%
        assertThat(report.tonnage().before()).isEqualByComparingTo("4800");
        assertThat(report.tonnage().after()).isEqualByComparingTo("5500");
        assertThat(report.tonnage().changePercent()).isEqualByComparingTo("14.6");

        // тренировки считаются по уникальным workoutId: в сентябре 3, в августе 2
        assertThat(report.workouts().count()).isEqualTo(3);
        assertThat(report.workouts().previousCount()).isEqualTo(2);
        assertThat(report.workouts().changePercent()).isEqualByComparingTo("50.0");
    }

    @Test
    void newExerciseThisMonth_beforeIsFirstResultOfMonth() {
        List<ExercisePerformance> history = List.of(
                perf(bench, "Жим лёжа", UUID.randomUUID(), "2026-09-03", "80", "1000"),
                perf(bench, "Жим лёжа", UUID.randomUUID(), "2026-09-20", "84", "1000"));

        MonthlyReportResponse report = MonthlyReportCalculator.build(SEPTEMBER, history, 0);

        ExerciseMonthResponse row = report.exercises().get(0);
        assertThat(row.beforeE1rmKg()).isEqualByComparingTo("80");
        assertThat(row.afterE1rmKg()).isEqualByComparingTo("84");
        assertThat(report.tonnage().before()).isNull();         // в августе тренировок не было
        assertThat(report.tonnage().changePercent()).isNull();
    }

    @Test
    void noWorkoutsThisMonth_emptyReport() {
        List<ExercisePerformance> history = List.of(
                perf(bench, "Жим лёжа", UUID.randomUUID(), "2026-08-20", "90", "2000"));

        MonthlyReportResponse report = MonthlyReportCalculator.build(SEPTEMBER, history, 0);

        assertThat(report.empty()).isTrue();
        assertThat(report.strength()).isNull();
        assertThat(report.exercises()).isEmpty();
        assertThat(report.workouts().previousCount()).isEqualTo(1);
    }

    @Test
    void monthBoundaries_areUtc() {
        assertThat(MonthlyReportCalculator.startOf(SEPTEMBER)).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
    }

    private ExercisePerformance perf(UUID exerciseId, String name, UUID workoutId, String date, String e1rm, String volume) {
        return ExercisePerformance.builder()
                .userId(UUID.randomUUID())
                .workoutId(workoutId)
                .exerciseId(exerciseId)
                .exerciseName(name)
                .bodyPart(BodyPart.CHEST)
                .trackingType("WEIGHT_REPS")
                .performedAt(Instant.parse(date + "T18:00:00Z"))
                .workingSets(3)
                .volumeKg(new BigDecimal(volume))
                .bestE1rmKg(e1rm == null ? null : new BigDecimal(e1rm))
                .build();
    }
}
