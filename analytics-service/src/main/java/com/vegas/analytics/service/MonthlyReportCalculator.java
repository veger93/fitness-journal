package com.vegas.analytics.service;

import com.vegas.analytics.dto.ExerciseMonthResponse;
import com.vegas.analytics.dto.MetricChangeResponse;
import com.vegas.analytics.dto.MonthlyReportResponse;
import com.vegas.analytics.dto.WorkoutsCountResponse;
import com.vegas.analytics.entity.ExercisePerformance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Отчёт за месяц из истории за этот и прошлый месяц. Чистая функция, как InsightsCalculator.
 * Границы месяца — по UTC (часовой пояс пользователя пока не храним).
 */
public final class MonthlyReportCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private MonthlyReportCalculator() {
    }

    public static Instant startOf(YearMonth month) {
        return month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * @param history     тренировки за прошлый и этот месяц, по возрастанию даты
     * @param recordsCount сколько рекордов поставлено в этом месяце
     */
    public static MonthlyReportResponse build(YearMonth month, List<ExercisePerformance> history, int recordsCount) {
        Instant start = startOf(month);
        Instant end = startOf(month.plusMonths(1));
        Instant previousStart = startOf(month.minusMonths(1));

        List<ExercisePerformance> current = between(history, start, end);
        List<ExercisePerformance> previous = between(history, previousStart, start);

        WorkoutsCountResponse workouts = workouts(current, previous);
        if (current.isEmpty()) {
            return new MonthlyReportResponse(month.toString(), true, null, null, workouts, 0, List.of());
        }

        List<ExerciseMonthResponse> exercises = exercises(current, previous);
        return new MonthlyReportResponse(month.toString(), false, strength(exercises), tonnage(current, previous),
                workouts, recordsCount, exercises);
    }

    // ---------- по упражнениям ----------

    private static List<ExerciseMonthResponse> exercises(List<ExercisePerformance> current, List<ExercisePerformance> previous) {
        // LinkedHashMap — сохраняем порядок появления, чтобы при равенстве сортировка была стабильной
        Map<UUID, List<ExercisePerformance>> byExercise = current.stream()
                .collect(Collectors.groupingBy(ExercisePerformance::getExerciseId, LinkedHashMap::new, Collectors.toList()));

        return byExercise.values().stream()
                .map(sessions -> toRow(sessions, previous))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(ExerciseMonthResponse::sessions).reversed()
                        .thenComparing(ExerciseMonthResponse::exerciseName))
                .toList();
    }

    /** Строка только для упражнений с расчётным 1ПМ: у подтягиваний без веса сравнивать нечего. */
    private static Optional<ExerciseMonthResponse> toRow(List<ExercisePerformance> sessions, List<ExercisePerformance> previous) {
        ExercisePerformance any = sessions.get(0);
        Optional<BigDecimal> after = bestE1rm(sessions);
        if (after.isEmpty()) {
            return Optional.empty();
        }
        List<ExercisePerformance> sameExerciseBefore = previous.stream()
                .filter(p -> p.getExerciseId().equals(any.getExerciseId()))
                .toList();
        // было: лучший результат прошлого месяца, а если упражнения тогда не было — первый в этом
        BigDecimal before = bestE1rm(sameExerciseBefore).orElseGet(() -> sessions.stream()
                .map(ExercisePerformance::getBestE1rmKg)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow());

        return Optional.of(new ExerciseMonthResponse(any.getExerciseId(), any.getExerciseName(),
                before, after.get(), percent(before, after.get()), sessions.size()));
    }

    // ---------- сводка ----------

    /** "Сила в среднем": среднее "было" и "стало" по всем упражнениям с 1ПМ. */
    private static MetricChangeResponse strength(List<ExerciseMonthResponse> exercises) {
        if (exercises.isEmpty()) {
            return null;
        }
        BigDecimal before = average(exercises.stream().map(ExerciseMonthResponse::beforeE1rmKg).toList());
        BigDecimal after = average(exercises.stream().map(ExerciseMonthResponse::afterE1rmKg).toList());
        return new MetricChangeResponse(before, after, percent(before, after),
                "средний расчётный максимум на 1 повтор по " + exercises.size() + " " + exercisesWord(exercises.size()));
    }

    private static MetricChangeResponse tonnage(List<ExercisePerformance> current, List<ExercisePerformance> previous) {
        BigDecimal after = volume(current);
        BigDecimal before = previous.isEmpty() ? null : volume(previous);
        return new MetricChangeResponse(before, after, percent(before, after),
                "сумма «вес × повторы» рабочих подходов за месяц");
    }

    private static WorkoutsCountResponse workouts(List<ExercisePerformance> current, List<ExercisePerformance> previous) {
        int count = distinctWorkouts(current);
        int previousCount = distinctWorkouts(previous);
        BigDecimal change = previousCount == 0 ? null
                : percent(BigDecimal.valueOf(previousCount), BigDecimal.valueOf(count));
        return new WorkoutsCountResponse(count, previousCount, change, "в прошлом месяце — " + previousCount);
    }

    // ---------- helpers ----------

    private static List<ExercisePerformance> between(List<ExercisePerformance> history, Instant from, Instant to) {
        return history.stream()
                .filter(p -> !p.getPerformedAt().isBefore(from) && p.getPerformedAt().isBefore(to))
                .toList();
    }

    private static Optional<BigDecimal> bestE1rm(List<ExercisePerformance> sessions) {
        return sessions.stream().map(ExercisePerformance::getBestE1rmKg).filter(Objects::nonNull).max(BigDecimal::compareTo);
    }

    private static BigDecimal volume(List<ExercisePerformance> sessions) {
        return sessions.stream().map(ExercisePerformance::getVolumeKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int distinctWorkouts(List<ExercisePerformance> sessions) {
        return (int) sessions.stream().map(ExercisePerformance::getWorkoutId).distinct().count();
    }

    private static BigDecimal average(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    /** (стало − было) / было × 100; было нет или 0 -> null. */
    static BigDecimal percent(BigDecimal before, BigDecimal after) {
        if (before == null || after == null || before.signum() == 0) {
            return null;
        }
        return after.subtract(before).multiply(HUNDRED).divide(before, 1, RoundingMode.HALF_UP);
    }

    /** 1 упражнению, 2 упражнениям... — для подсказки "по N упражнениям". */
    private static String exercisesWord(int n) {
        return n % 10 == 1 && n % 100 != 11 ? "упражнению" : "упражнениям";
    }
}
