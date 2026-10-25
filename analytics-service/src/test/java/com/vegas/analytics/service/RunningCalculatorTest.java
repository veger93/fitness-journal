package com.vegas.analytics.service;

import com.vegas.analytics.dto.RunningSummaryResponse;
import com.vegas.analytics.dto.RunningSummaryResponse.Effort;
import com.vegas.analytics.dto.RunningSummaryResponse.Load;
import com.vegas.analytics.dto.RunningSummaryResponse.Run;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** NOW — суббота 2026-10-24, неделя началась в понедельник 2026-10-19. */
class RunningCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");
    /** Пробежки из run() отсчитаны от этого момента — для тестов главной. */
    static final Instant NOW_FOR_DASHBOARD = NOW;

    @Test
    void noRuns_isEmpty() {
        RunningSummaryResponse summary = RunningCalculator.summary(List.of(), NOW);

        assertThat(summary.empty()).isTrue();
        assertThat(summary.weeks()).hasSize(8).allMatch(week -> week.distanceM() == 0);
    }

    @Test
    void weeks_startOnMonday_lastIsCurrent() {
        List<ExercisePerformance> runs = List.of(run(1, 5000, 1800, null), run(8, 7000, 2400, null));

        var weeks = RunningCalculator.weeks(runs, NOW);

        assertThat(weeks.get(7).weekStart()).isEqualTo(LocalDate.parse("2026-10-19"));
        assertThat(weeks.get(7).distanceM()).isEqualTo(5000);
        assertThat(weeks.get(6).distanceM()).isEqualTo(7000);
    }

    @Test
    void load_over30percent_isWarning() {
        // 2 недели назад и раньше: 10 км; последние 2 недели: 15 км (+50%)
        List<ExercisePerformance> runs = List.of(
                run(25, 5000, 1800, null), run(18, 5000, 1800, null),
                run(10, 7000, 2500, null), run(3, 8000, 2900, null));

        Load load = RunningCalculator.load(runs, NOW);

        assertThat(load.changePercent()).isEqualByComparingTo("50");
        assertThat(load.warning()).isTrue();
        assertThat(load.hint()).contains("50%").contains("10 → 15 км");
    }

    @Test
    void load_smallBase_isNotWarning() {
        // 2 -> 4 км — "+100%", но с такой малой базы проценты ничего не значат
        Load load = RunningCalculator.load(List.of(run(20, 2000, 700, null), run(3, 4000, 1400, null)), NOW);

        assertThat(load.warning()).isFalse();
    }

    @Test
    void records_longestAndBestPace_freshOnlyIfNotFirstRun() {
        List<ExercisePerformance> runs = List.of(
                run(30, 5000, 1800, null),                  // 6:00 /км
                run(5, 10000, 3300, null));                 // 5:30 /км и самая длинная

        Run longest = RunningCalculator.longestRun(runs, NOW).orElseThrow();
        Run pace = RunningCalculator.bestPace(runs, NOW).orElseThrow();

        assertThat(longest.distanceM()).isEqualTo(10000);
        assertThat(longest.fresh()).isTrue();
        assertThat(pace.paceSecPerKm()).isEqualByComparingTo("330");
        assertThat(RunningCalculator.longestRun(List.of(run(2, 5000, 1800, null)), NOW).orElseThrow().fresh())
                .isFalse(); // первая пробежка — сравнивать не с чем
    }

    @Test
    void effort_fewEasyRuns_givesHint() {
        List<ExercisePerformance> runs = List.of(
                run(20, 5000, 1800, "8"), run(14, 5000, 1800, "6"), run(7, 5000, 1800, "8"),
                run(3, 5000, 1800, "4"), run(1, 5000, 1800, null));

        Effort effort = RunningCalculator.effort(runs, NOW);

        assertThat(effort.easy()).isEqualTo(1);
        assertThat(effort.moderate()).isEqualTo(1);
        assertThat(effort.hard()).isEqualTo(2);
        assertThat(effort.unrated()).isEqualTo(1);
        assertThat(effort.hint()).isNotNull();
    }

    @Test
    void speedAndPaceFormatting() {
        assertThat(RunningCalculator.speedKmh(run(1, 5000, 2250, null))).isEqualByComparingTo("8");
        assertThat(RunningCalculator.pace(new BigDecimal("290"))).isEqualTo("4:50");
        assertThat(RunningCalculator.km(12500)).isEqualTo("12,5");
    }

    /** Пробежка daysAgo дней назад; темп считается от дистанции и времени, как в PerformanceCalculator. */
    static ExercisePerformance run(int daysAgo, int distanceM, int durationSec, String rpe) {
        return ExercisePerformance.builder()
                .userId(UUID.randomUUID())
                .workoutId(UUID.randomUUID())
                .exerciseId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .exerciseName("Бег")
                .bodyPart(BodyPart.LEGS)
                .trackingType("DISTANCE")
                .performedAt(NOW.minus(Duration.ofDays(daysAgo)))
                .workingSets(1)
                .volumeKg(BigDecimal.ZERO)
                .bestDistanceM(distanceM)
                .bestDurationSec(durationSec)
                .totalDistanceM(distanceM)
                .totalDurationSec(durationSec)
                .bestPaceSecPerKm(distanceM >= 1000
                        ? BigDecimal.valueOf(durationSec * 1000L).divide(BigDecimal.valueOf(distanceM), 1, java.math.RoundingMode.HALF_UP)
                        : null)
                .avgRpe(rpe == null ? null : new BigDecimal(rpe))
                .build();
    }
}
