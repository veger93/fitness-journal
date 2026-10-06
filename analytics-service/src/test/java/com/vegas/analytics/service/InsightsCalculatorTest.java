package com.vegas.analytics.service;

import com.vegas.analytics.dto.DeloadPlanResponse;
import com.vegas.analytics.dto.OverloadRiskResponse;
import com.vegas.analytics.dto.PlateauResponse;
import com.vegas.analytics.dto.RecommendationResponse;
import com.vegas.analytics.dto.RecommendationType;
import com.vegas.analytics.dto.RiskLevel;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Nested — группы тестов внутри одного класса: в отчёте IDEA они показываются деревом.
 */
class InsightsCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
    private static final UUID BENCH = UUID.randomUUID();

    @Nested
    class Plateau {

        @Test
        void noNewMaxFor30DaysWith3Sessions_isPlateau() {
            List<ExercisePerformance> history = List.of(
                    perf(40, "100"),
                    perf(30, "105"),   // последний максимум
                    perf(20, "104"),
                    perf(12, "103"),
                    perf(5, "105"));   // повтор максимума — не прогресс

            PlateauResponse plateau = InsightsCalculator.plateau(history, NOW);

            assertThat(plateau.detected()).isTrue();
            assertThat(plateau.daysSinceProgress()).isEqualTo(30);
            assertThat(plateau.sessionsSinceProgress()).isEqualTo(3);
            assertThat(plateau.lastProgressAt()).isEqualTo(NOW.minus(Duration.ofDays(30)));
        }

        @Test
        void longBreakWithoutTraining_isNotPlateau() {
            List<ExercisePerformance> history = List.of(perf(45, "100"), perf(40, "102"), perf(35, "103"), perf(30, "105"));

            assertThat(InsightsCalculator.plateau(history, NOW).detected()).isFalse(); // 0 тренировок после максимума
        }

        @Test
        void steadyGrowth_isNotPlateau() {
            List<ExercisePerformance> history = List.of(perf(30, "100"), perf(20, "102"), perf(10, "104"), perf(2, "106"));

            PlateauResponse plateau = InsightsCalculator.plateau(history, NOW);

            assertThat(plateau.detected()).isFalse();
            assertThat(plateau.daysSinceProgress()).isEqualTo(2);
        }
    }

    @Nested
    class OverloadRisk {

        @Test
        void volumeSpike_plusE1rmDrop_plusPlateau_isHigh() {
            List<ExercisePerformance> history = List.of(
                    perf(25, "110", "1000", null),
                    perf(18, "110", "1000", null),
                    perf(11, "108", "1000", null),
                    perf(3, "100", "2500", null));   // объём резко вырос, а 1ПМ упал
            PlateauResponse plateau = InsightsCalculator.plateau(history, NOW);

            OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, NOW, plateau);

            // неделя 2500 против среднего (5500 / 4 = 1375) -> 1.82
            assertThat(risk.acuteChronicRatio()).isEqualByComparingTo("1.82");
            // (110 - 100) / 110 -> 9.1%
            assertThat(risk.e1rmDropPercent()).isEqualByComparingTo("9.1");
            assertThat(risk.level()).isEqualTo(RiskLevel.HIGH);
            assertThat(risk.reasons()).hasSize(3);
        }

        @Test
        void highRpeOnly_isLow_butReasonIsShown() {
            List<ExercisePerformance> history = List.of(
                    perf(20, "100", "1000", null),
                    perf(13, "102", "1000", null),
                    perf(6, "104", "1000", "9.5"),
                    perf(2, "105", "1000", "9"));

            OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, NOW, InsightsCalculator.plateau(history, NOW));

            assertThat(risk.recentAvgRpe()).isEqualByComparingTo("9.3"); // (9.5 + 9) / 2 = 9.25 -> 9.3
            assertThat(risk.level()).isEqualTo(RiskLevel.LOW);           // 1 балл
            assertThat(risk.reasons()).singleElement().asString().contains("RPE");
        }

        @Test
        void shortHistory_noRatio() {
            List<ExercisePerformance> history = List.of(perf(10, "100", "1000", null), perf(2, "102", "3000", null));

            assertThat(InsightsCalculator.acuteChronicRatio(history, NOW)).isNull();
        }
    }

    @Nested
    class Recommendation {

        @Test
        void fewSessions_notEnoughData() {
            List<ExercisePerformance> history = List.of(perf(5, "100"), perf(2, "101"));
            PlateauResponse plateau = InsightsCalculator.plateau(history, NOW);
            OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, NOW, plateau);

            RecommendationResponse recommendation = InsightsCalculator.recommend(history.size(), plateau, risk);

            assertThat(recommendation.type()).isEqualTo(RecommendationType.NOT_ENOUGH_DATA);
            assertThat(recommendation.message()).contains("ещё 2");
        }

        @Test
        void plateau_suggestsDeload() {
            List<ExercisePerformance> history = List.of(perf(40, "100"), perf(30, "105"), perf(20, "104"), perf(12, "103"), perf(5, "104"));
            PlateauResponse plateau = InsightsCalculator.plateau(history, NOW);
            OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, NOW, plateau);

            assertThat(InsightsCalculator.recommend(history.size(), plateau, risk).type())
                    .isEqualTo(RecommendationType.DELOAD);
        }

        @Test
        void steadyGrowth_keepGoing() {
            List<ExercisePerformance> history = List.of(perf(30, "100"), perf(20, "102"), perf(10, "104"), perf(2, "106"));
            PlateauResponse plateau = InsightsCalculator.plateau(history, NOW);
            OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, NOW, plateau);

            assertThat(InsightsCalculator.recommend(history.size(), plateau, risk).type())
                    .isEqualTo(RecommendationType.KEEP_GOING);
        }
    }

    @Nested
    class DeloadPlan {

        @Test
        void seventyPercentOfE1rm_halfTheSets() {
            List<ExercisePerformance> history = List.of(
                    perf(20, "100", 4, 8),
                    perf(13, "101", 4, 8),
                    perf(6, "100", 5, 6),
                    perf(2, "101.33", 4, 8));

            DeloadPlanResponse plan = InsightsCalculator.deloadPlan(history, "причина");

            assertThat(plan.durationDays()).isEqualTo(7);
            assertThat(plan.currentE1rmKg()).isEqualByComparingTo("101.33");
            assertThat(plan.workingWeightKg()).isEqualByComparingTo("70");  // 70.93 -> ближайшие 2.5 кг
            assertThat(plan.sets()).isEqualTo(2);                           // обычно 4 -> 2
            assertThat(plan.reps()).isEqualTo(8);
            assertThat(plan.reason()).isEqualTo("причина");
        }

        @Test
        void bodyweightExercise_noWorkingWeight() {
            ExercisePerformance pullUps = base(3).bestReps(12).workingSets(3).build();

            DeloadPlanResponse plan = InsightsCalculator.deloadPlan(List.of(pullUps), "причина");

            assertThat(plan.workingWeightKg()).isNull();
            assertThat(plan.sets()).isEqualTo(2);   // 3 / 2 -> 1.5 -> вверх до 2
            assertThat(plan.reps()).isEqualTo(12);
        }

        @Test
        void roundToPlates() {
            assertThat(InsightsCalculator.roundToPlates(new BigDecimal("63.7"))).isEqualByComparingTo("62.5");
            assertThat(InsightsCalculator.roundToPlates(new BigDecimal("64.0"))).isEqualByComparingTo("65");
        }
    }

    // ---------- helpers ----------

    private static ExercisePerformance perf(int daysAgo, String e1rm) {
        return perf(daysAgo, e1rm, "1000", null);
    }

    private static ExercisePerformance perf(int daysAgo, String e1rm, String volume, String avgRpe) {
        return base(daysAgo)
                .bestE1rmKg(new BigDecimal(e1rm))
                .volumeKg(new BigDecimal(volume))
                .avgRpe(avgRpe == null ? null : new BigDecimal(avgRpe))
                .workingSets(3)
                .bestReps(8)
                .build();
    }

    private static ExercisePerformance perf(int daysAgo, String e1rm, int sets, int reps) {
        return base(daysAgo)
                .bestE1rmKg(new BigDecimal(e1rm))
                .volumeKg(new BigDecimal("1000"))
                .workingSets(sets)
                .bestReps(reps)
                .build();
    }

    private static ExercisePerformance.ExercisePerformanceBuilder base(int daysAgo) {
        return ExercisePerformance.builder()
                .userId(UUID.randomUUID())
                .workoutId(UUID.randomUUID())
                .exerciseId(BENCH)
                .exerciseName("Жим лёжа")
                .bodyPart(BodyPart.CHEST)
                .trackingType("WEIGHT_REPS")
                .performedAt(NOW.minus(Duration.ofDays(daysAgo)))
                .volumeKg(BigDecimal.ZERO);
    }
}
