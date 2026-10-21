package com.vegas.analytics.service;

import com.vegas.analytics.dto.DashboardResponse;
import com.vegas.analytics.dto.ExerciseStatus;
import com.vegas.analytics.dto.ExerciseSummaryResponse;
import com.vegas.analytics.dto.OverloadRiskResponse;
import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.dto.PlateauResponse;
import com.vegas.analytics.dto.RecordItemResponse;
import com.vegas.analytics.dto.RecordType;
import com.vegas.analytics.dto.RiskLevel;
import com.vegas.analytics.entity.ExercisePerformance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Сводка для главной. Чистые функции, как InsightsCalculator: вся история передаётся параметром,
 * "сейчас" — тоже, поэтому всё проверяется тестами без БД.
 */
public final class DashboardCalculator {

    /** Сколько последних тренировок показывать в мини-графике. */
    static final int TREND_POINTS = 8;

    /** Рекорд считается "свежим" (бейдж на главной) две недели. */
    static final Duration FRESH_RECORD = Duration.ofDays(14);

    private static final Duration WEEK = Duration.ofDays(7);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private DashboardCalculator() {
    }

    /**
     * @param history все выполнения пользователя по возрастанию даты
     * @param records карточки рекордов, новые сверху (RecordsService)
     */
    public static DashboardResponse build(List<ExercisePerformance> history, List<PersonalRecordResponse> records, Instant now) {
        YearMonth month = YearMonth.from(now.atZone(ZoneOffset.UTC));
        if (history.isEmpty()) {
            return new DashboardResponse(true, month.toString(), 0, BigDecimal.ZERO, BigDecimal.ZERO, null, null, List.of());
        }

        BigDecimal week = tonnageBetween(history, now.minus(WEEK), now);
        BigDecimal previousWeek = tonnageBetween(history, now.minus(WEEK.multipliedBy(2)), now.minus(WEEK));
        BigDecimal change = previousWeek.signum() == 0 ? null
                : week.subtract(previousWeek).multiply(HUNDRED).divide(previousWeek, 1, RoundingMode.HALF_UP);

        Instant monthStart = MonthlyReportCalculator.startOf(month);
        int workouts = (int) history.stream()
                .filter(p -> !p.getPerformedAt().isBefore(monthStart))
                .map(ExercisePerformance::getWorkoutId)
                .distinct()
                .count();

        List<ExerciseSummaryResponse> exercises = summaries(history, records, now);
        UUID focus = exercises.stream()
                .filter(e -> e.currentE1rmKg() != null)
                .findFirst()
                .or(() -> exercises.stream().findFirst())
                .map(ExerciseSummaryResponse::exerciseId)
                .orElse(null);

        return new DashboardResponse(false, month.toString(), workouts, scale(week), scale(previousWeek), change, focus, exercises);
    }

    static List<ExerciseSummaryResponse> summaries(List<ExercisePerformance> history, List<PersonalRecordResponse> records,
                                                   Instant now) {
        // группируем по упражнению, сохраняя порядок дат внутри группы
        Map<UUID, List<ExercisePerformance>> byExercise = new LinkedHashMap<>();
        for (ExercisePerformance performance : history) {
            byExercise.computeIfAbsent(performance.getExerciseId(), id -> new ArrayList<>()).add(performance);
        }

        List<ExerciseSummaryResponse> result = new ArrayList<>();
        for (List<ExercisePerformance> exerciseHistory : byExercise.values()) {
            result.add(summary(exerciseHistory, freshRecord(records, exerciseHistory.get(0).getExerciseId(), now), now));
        }
        result.sort(Comparator.comparing(ExerciseSummaryResponse::status)
                .thenComparing(ExerciseSummaryResponse::lastPerformedAt, Comparator.reverseOrder()));
        return result;
    }

    private static ExerciseSummaryResponse summary(List<ExercisePerformance> history, Optional<PersonalRecordResponse> record,
                                                   Instant now) {
        ExercisePerformance last = history.get(history.size() - 1);
        PlateauResponse plateau = InsightsCalculator.plateau(history, now);
        OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, now, plateau);

        ExerciseStatus status;
        String note;
        if (risk.level() == RiskLevel.HIGH) {
            status = ExerciseStatus.OVERLOAD;
            note = risk.reasons().isEmpty() ? "Признаки накопленной усталости" : risk.reasons().get(0);
        } else if (plateau.detected()) {
            status = ExerciseStatus.PLATEAU;
            note = plateau.daysSinceProgress() + " дн. без нового максимума";
        } else if (record.isPresent()) {
            status = ExerciseStatus.RECORD;
            note = recordNote(record.get());
        } else {
            status = ExerciseStatus.NORMAL;
            note = null;
        }

        List<BigDecimal> e1rms = history.stream()
                .map(ExercisePerformance::getBestE1rmKg)
                .filter(Objects::nonNull)
                .toList();
        BigDecimal currentE1rm = e1rms.isEmpty() ? null : e1rms.get(e1rms.size() - 1);
        List<BigDecimal> trend = e1rms.subList(Math.max(0, e1rms.size() - TREND_POINTS), e1rms.size());

        // без веса (подтягивания) показываем лучшие повторы, а не прочерк
        Integer lastReps = last.getMaxWeightReps() != null ? last.getMaxWeightReps() : last.getBestReps();
        return new ExerciseSummaryResponse(last.getExerciseId(), last.getExerciseName(), last.getTrackingType(),
                last.getPerformedAt(), last.getMaxWeightKg(), lastReps, last.getBestDistanceM(), last.getBestDurationSec(),
                currentE1rm, status, risk.level(), note, trend);
    }

    private static Optional<PersonalRecordResponse> freshRecord(List<PersonalRecordResponse> records, UUID exerciseId, Instant now) {
        Instant from = now.minus(FRESH_RECORD);
        return records.stream()
                .filter(r -> r.exerciseId().equals(exerciseId) && !r.achievedAt().isBefore(from))
                .findFirst(); // records — новые сверху
    }

    /** "+5 кг к прошлому лучшему": рекорд веса понятнее, его берём первым. */
    static String recordNote(PersonalRecordResponse record) {
        RecordItemResponse item = record.records().stream()
                .filter(i -> i.type() == RecordType.WEIGHT)
                .findFirst()
                .orElse(record.records().get(0));
        if (item.deltaKg() == null) {
            return "Новый рекорд";
        }
        return "+" + item.deltaKg().stripTrailingZeros().toPlainString() + " кг к прошлому лучшему";
    }

    private static BigDecimal tonnageBetween(List<ExercisePerformance> history, Instant from, Instant to) {
        return history.stream()
                .filter(p -> !p.getPerformedAt().isBefore(from) && p.getPerformedAt().isBefore(to))
                .map(ExercisePerformance::getVolumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }
}
