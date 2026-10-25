package com.vegas.analytics.service;

import com.vegas.analytics.dto.RunningSummaryResponse;
import com.vegas.analytics.dto.RunningSummaryResponse.Effort;
import com.vegas.analytics.dto.RunningSummaryResponse.Load;
import com.vegas.analytics.dto.RunningSummaryResponse.Run;
import com.vegas.analytics.dto.RunningSummaryResponse.Week;
import com.vegas.analytics.entity.ExercisePerformance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Аналитика бега. Чистые функции, как InsightsCalculator: история и "сейчас" — параметрами.
 *
 * Источники (docs/references.md):
 * - рост километража > 30% за две недели — возможный риск "дистанционных" травм у начинающих [12];
 * - основа тренировок бегуна — лёгкий бег, ~80% пробежек [11]; "Легко / Средне / Тяжело" — по тесту разговором [13].
 */
public final class RunningCalculator {

    static final int WEEKS_SHOWN = 8;
    static final BigDecimal LOAD_WARNING_PERCENT = new BigDecimal("30");
    /** С совсем малых объёмов проценты обманчивы: 2 -> 4 км это "+100%", но не повод тревожиться. */
    static final int LOAD_MIN_BASE_M = 5000;
    static final Duration FRESH = Duration.ofDays(14);
    /** Отметка RPE: легко ≤ 4.5, средне ≤ 6.5, дальше — тяжело (кнопки дают 4 / 6 / 8). */
    static final BigDecimal EASY_MAX = new BigDecimal("4.5");
    static final BigDecimal MODERATE_MAX = new BigDecimal("6.5");
    /** Подсказку про баланс даём, когда отметок хватает, чтобы делать выводы. */
    static final int EFFORT_MIN_RATED = 4;

    private static final Duration TWO_WEEKS = Duration.ofDays(14);
    private static final Duration FOUR_WEEKS = Duration.ofDays(28);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private RunningCalculator() {
    }

    /** @param runs выполнения упражнений на дистанцию по возрастанию даты */
    public static RunningSummaryResponse summary(List<ExercisePerformance> runs, Instant now) {
        if (runs.isEmpty()) {
            return new RunningSummaryResponse(true, weeks(runs, now), load(runs, now), null, null, effort(runs, now));
        }
        return new RunningSummaryResponse(false, weeks(runs, now), load(runs, now),
                longestRun(runs, now).orElse(null), bestPace(runs, now).orElse(null), effort(runs, now));
    }

    static List<Week> weeks(List<ExercisePerformance> runs, Instant now) {
        LocalDate currentWeek = now.atZone(ZoneOffset.UTC).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Week> weeks = new ArrayList<>();
        for (int i = WEEKS_SHOWN - 1; i >= 0; i--) {
            LocalDate start = currentWeek.minusWeeks(i);
            Instant from = start.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = start.plusWeeks(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            weeks.add(new Week(start, distanceBetween(runs, from, to)));
        }
        return weeks;
    }

    static Load load(List<ExercisePerformance> runs, Instant now) {
        int last = distanceBetween(runs, now.minus(TWO_WEEKS), now.plusSeconds(1));
        int previous = distanceBetween(runs, now.minus(FOUR_WEEKS), now.minus(TWO_WEEKS));
        if (previous == 0) {
            return new Load(last, previous, null, false, null);
        }
        BigDecimal change = BigDecimal.valueOf(last - previous).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(previous), 0, RoundingMode.HALF_UP);
        boolean warning = previous >= LOAD_MIN_BASE_M && change.compareTo(LOAD_WARNING_PERCENT) > 0;
        String hint = warning
                ? "За две недели километраж вырос на " + change + "% (" + km(previous) + " → " + km(last)
                + " км). Резкий рост объёма может повышать риск травм — лучше прибавлять плавнее."
                : null;
        return new Load(last, previous, change, warning, hint);
    }

    static Optional<Run> longestRun(List<ExercisePerformance> runs, Instant now) {
        return record(runs, ExercisePerformance::getTotalDistanceM, Comparator.naturalOrder(), now);
    }

    /** Темп: меньше — лучше, поэтому ищем минимум. */
    static Optional<Run> bestPace(List<ExercisePerformance> runs, Instant now) {
        return record(runs, ExercisePerformance::getBestPaceSecPerKm, Comparator.reverseOrder(), now);
    }

    /**
     * Рекорд = лучшее значение за всё время. Свежий — если поставлен недавно и до него уже были пробежки
     * (первая пробежка рекордом не считается: сравнивать не с чем — как у силовых рекордов).
     * При равенстве побеждает более ранняя пробежка: повтор рекорда — не рекорд.
     */
    private static <T extends Comparable<? super T>> Optional<Run> record(List<ExercisePerformance> runs,
                                                                          Function<ExercisePerformance, T> value,
                                                                          Comparator<T> better, Instant now) {
        ExercisePerformance best = null;
        for (ExercisePerformance run : runs) {
            T v = value.apply(run);
            if (v != null && (best == null || better.compare(v, value.apply(best)) > 0)) {
                best = run;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        ExercisePerformance record = best;
        boolean hadEarlier = runs.stream().anyMatch(r -> r.getPerformedAt().isBefore(record.getPerformedAt()) && value.apply(r) != null);
        boolean fresh = hadEarlier && !record.getPerformedAt().isBefore(now.minus(FRESH));
        return Optional.of(new Run(record.getPerformedAt(), record.getTotalDistanceM(), record.getTotalDurationSec(),
                record.getBestPaceSecPerKm(), fresh));
    }

    static Effort effort(List<ExercisePerformance> runs, Instant now) {
        Instant from = now.minus(FOUR_WEEKS);
        int easy = 0;
        int moderate = 0;
        int hard = 0;
        int unrated = 0;
        for (ExercisePerformance run : runs) {
            if (run.getPerformedAt().isBefore(from)) {
                continue;
            }
            BigDecimal rpe = run.getAvgRpe();
            if (rpe == null) {
                unrated++;
            } else if (rpe.compareTo(EASY_MAX) <= 0) {
                easy++;
            } else if (rpe.compareTo(MODERATE_MAX) <= 0) {
                moderate++;
            } else {
                hard++;
            }
        }
        int rated = easy + moderate + hard;
        String hint = rated >= EFFORT_MIN_RATED && easy * 2 < rated
                ? "Лёгких пробежек меньше половины. У бегунов основа — спокойный бег, когда можно разговаривать:"
                + " так растёт выносливость, а тело успевает восстановиться. Добавьте лёгких."
                : null;
        return new Effort(easy, moderate, hard, unrated, hint);
    }

    /** Средняя скорость пробежки, км/ч (для мини-графика). null — нет дистанции или времени. */
    static BigDecimal speedKmh(ExercisePerformance run) {
        if (run.getTotalDistanceM() == null || run.getTotalDurationSec() == null || run.getTotalDurationSec() == 0) {
            return null;
        }
        return BigDecimal.valueOf(run.getTotalDistanceM() * 3.6)
                .divide(BigDecimal.valueOf(run.getTotalDurationSec()), 1, RoundingMode.HALF_UP);
    }

    /** 290 сек/км -> "4:50" */
    static String pace(BigDecimal secPerKm) {
        int total = secPerKm.setScale(0, RoundingMode.HALF_UP).intValue();
        return total / 60 + ":" + String.format("%02d", total % 60);
    }

    static String km(int meters) {
        return BigDecimal.valueOf(meters).divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static int distanceBetween(List<ExercisePerformance> runs, Instant from, Instant to) {
        return runs.stream()
                .filter(r -> !r.getPerformedAt().isBefore(from) && r.getPerformedAt().isBefore(to))
                .map(ExercisePerformance::getTotalDistanceM)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
