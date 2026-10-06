package com.vegas.analytics.service;

import com.vegas.analytics.dto.DeloadPlanResponse;
import com.vegas.analytics.dto.OverloadRiskResponse;
import com.vegas.analytics.dto.PlateauResponse;
import com.vegas.analytics.dto.RecommendationResponse;
import com.vegas.analytics.dto.RecommendationType;
import com.vegas.analytics.dto.RiskLevel;
import com.vegas.analytics.entity.ExercisePerformance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Эвристики "умного дневника". Чистые функции: история упражнения + "сейчас" -> выводы.
 * Без БД и Spring, поэтому каждое правило легко проверить тестом.
 *
 * Источники порогов и формул: docs/references.md ([3]–[6]).
 *
 * Важно: это правила-ориентиры, а не медицина. Пороги вынесены в константы,
 * чтобы их было легко подстроить, когда появятся реальные данные пользователей.
 */
public final class InsightsCalculator {

    /** Плато: нет нового максимума 1ПМ столько дней... */
    static final int PLATEAU_MIN_DAYS = 21;
    /** ...и за это время было не меньше стольких тренировок (иначе это перерыв, а не плато). */
    static final int PLATEAU_MIN_SESSIONS = 3;
    /** Меньше тренировок — выводы делать рано. */
    static final int MIN_SESSIONS_FOR_ADVICE = 4;

    /** Нагрузка за неделю против средней недельной за 4 недели (acute:chronic workload ratio). */
    static final BigDecimal ACWR_HIGH = new BigDecimal("1.5");
    static final BigDecimal ACWR_ELEVATED = new BigDecimal("1.3");
    /** ACWR считаем, только если история упражнения не короче 3 недель, иначе "хроническая" нагрузка не показательна. */
    static final Duration ACWR_MIN_HISTORY = Duration.ofDays(21);

    static final BigDecimal DROP_HIGH_PERCENT = new BigDecimal("5");
    static final BigDecimal DROP_ELEVATED_PERCENT = new BigDecimal("2.5");
    static final BigDecimal RPE_HIGH = new BigDecimal("9");

    /** Разгрузка: ~7 дней, вес 70% от 1ПМ, подходов вдвое меньше. */
    static final int DELOAD_DAYS = 7;
    static final int DELOAD_INTENSITY_PERCENT = 70;
    static final BigDecimal PLATE_STEP_KG = new BigDecimal("2.5");
    static final int DEFAULT_REPS = 8;

    private static final Duration WEEK = Duration.ofDays(7);
    private static final Duration FOUR_WEEKS = Duration.ofDays(28);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private InsightsCalculator() {
    }

    // ---------- плато ----------

    /** @param history тренировки с упражнением по возрастанию даты */
    public static PlateauResponse plateau(List<ExercisePerformance> history, Instant now) {
        BigDecimal max = null;
        Instant lastProgressAt = null;
        for (ExercisePerformance performance : history) {
            BigDecimal e1rm = performance.getBestE1rmKg();
            if (e1rm != null && (max == null || e1rm.compareTo(max) > 0)) {
                max = e1rm;
                lastProgressAt = performance.getPerformedAt();
            }
        }
        if (lastProgressAt == null) {
            return new PlateauResponse(false, null, 0, 0);
        }

        Instant progressAt = lastProgressAt;
        int sessionsSince = (int) history.stream().filter(p -> p.getPerformedAt().isAfter(progressAt)).count();
        long days = Duration.between(lastProgressAt, now).toDays();

        boolean detected = history.size() >= MIN_SESSIONS_FOR_ADVICE
                && days >= PLATEAU_MIN_DAYS
                && sessionsSince >= PLATEAU_MIN_SESSIONS;
        return new PlateauResponse(detected, lastProgressAt, days, sessionsSince);
    }

    // ---------- риск перегрузки ----------

    /**
     * Баллы: резкий рост объёма (до 2), падение 1ПМ от недавнего пика (до 2),
     * подходы почти до отказа (1), плато (1). 0–1 LOW, 2–3 MEDIUM, 4+ HIGH.
     */
    public static OverloadRiskResponse overloadRisk(List<ExercisePerformance> history, Instant now, PlateauResponse plateau) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        BigDecimal acwr = acuteChronicRatio(history, now);
        if (acwr != null && acwr.compareTo(ACWR_HIGH) > 0) {
            score += 2;
            reasons.add("Объём за неделю в " + format(acwr) + " раза выше среднего за месяц");
        } else if (acwr != null && acwr.compareTo(ACWR_ELEVATED) > 0) {
            score += 1;
            reasons.add("Объём за неделю заметно выше обычного (×" + format(acwr) + ")");
        }

        BigDecimal drop = e1rmDropPercent(history);
        if (drop != null && drop.compareTo(DROP_HIGH_PERCENT) >= 0) {
            score += 2;
            reasons.add("1ПМ на " + format(drop) + "% ниже пика за последние 4 недели");
        } else if (drop != null && drop.compareTo(DROP_ELEVATED_PERCENT) >= 0) {
            score += 1;
            reasons.add("1ПМ немного просел: −" + format(drop) + "% от недавнего пика");
        }

        BigDecimal rpe = recentAverageRpe(history, now);
        if (rpe != null && rpe.compareTo(RPE_HIGH) >= 0) {
            score += 1;
            reasons.add("Подходы почти до отказа: средний RPE " + format(rpe) + " за неделю");
        }

        if (plateau.detected()) {
            score += 1;
            reasons.add("Нет роста 1ПМ " + plateau.daysSinceProgress() + " дн.");
        }

        RiskLevel level = score >= 4 ? RiskLevel.HIGH : score >= 2 ? RiskLevel.MEDIUM : RiskLevel.LOW;
        return new OverloadRiskResponse(level, acwr, drop, rpe, reasons);
    }

    /** Объём за последние 7 дней / (объём за 28 дней / 4). */
    static BigDecimal acuteChronicRatio(List<ExercisePerformance> history, Instant now) {
        if (history.isEmpty() || history.get(0).getPerformedAt().isAfter(now.minus(ACWR_MIN_HISTORY))) {
            return null;
        }
        BigDecimal acute = volumeSince(history, now.minus(WEEK));
        BigDecimal chronicWeekly = volumeSince(history, now.minus(FOUR_WEEKS)).divide(BigDecimal.valueOf(4), 4, RoundingMode.HALF_UP);
        if (chronicWeekly.signum() == 0) {
            return null; // упражнение без веса или долгий перерыв
        }
        return acute.divide(chronicWeekly, 2, RoundingMode.HALF_UP);
    }

    /** На сколько % последний 1ПМ ниже максимума за 4 недели ДО последней тренировки. 0 — не ниже. */
    static BigDecimal e1rmDropPercent(List<ExercisePerformance> history) {
        List<ExercisePerformance> withE1rm = history.stream().filter(p -> p.getBestE1rmKg() != null).toList();
        if (withE1rm.size() < 2) {
            return null;
        }
        ExercisePerformance last = withE1rm.get(withE1rm.size() - 1);
        Instant windowStart = last.getPerformedAt().minus(FOUR_WEEKS);
        BigDecimal peak = withE1rm.stream()
                .filter(p -> p != last && !p.getPerformedAt().isBefore(windowStart))
                .map(ExercisePerformance::getBestE1rmKg)
                .max(BigDecimal::compareTo)
                .orElse(null);
        if (peak == null || last.getBestE1rmKg().compareTo(peak) >= 0) {
            return peak == null ? null : BigDecimal.ZERO;
        }
        return peak.subtract(last.getBestE1rmKg()).multiply(HUNDRED).divide(peak, 1, RoundingMode.HALF_UP);
    }

    static BigDecimal recentAverageRpe(List<ExercisePerformance> history, Instant now) {
        Instant weekAgo = now.minus(WEEK);
        List<BigDecimal> rpes = history.stream()
                .filter(p -> p.getPerformedAt().isAfter(weekAgo))
                .map(ExercisePerformance::getAvgRpe)
                .filter(Objects::nonNull)
                .toList();
        if (rpes.isEmpty()) {
            return null;
        }
        return rpes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rpes.size()), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal volumeSince(List<ExercisePerformance> history, Instant from) {
        return history.stream()
                .filter(p -> p.getPerformedAt().isAfter(from))
                .map(ExercisePerformance::getVolumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------- рекомендация ----------

    /** Порядок важен: сначала самое серьёзное. */
    public static RecommendationResponse recommend(int sessions, PlateauResponse plateau, OverloadRiskResponse risk) {
        if (risk.level() == RiskLevel.HIGH) {
            return new RecommendationResponse(RecommendationType.DELOAD,
                    "Несколько признаков накопленной усталости. Неделя разгрузки поможет восстановиться "
                            + "и продолжить прогресс.");
        }
        if (plateau.detected()) {
            return new RecommendationResponse(RecommendationType.DELOAD,
                    "1ПМ не растёт " + plateau.daysSinceProgress() + " дн. при регулярных тренировках. "
                            + "Частый способ сдвинуться с плато — неделя разгрузки со сниженным объёмом и весом.");
        }
        if (sessions < MIN_SESSIONS_FOR_ADVICE) {
            int left = MIN_SESSIONS_FOR_ADVICE - sessions;
            return new RecommendationResponse(RecommendationType.NOT_ENOUGH_DATA,
                    "Нужно ещё " + left + " трен. с этим упражнением, чтобы делать выводы.");
        }
        if (risk.level() == RiskLevel.MEDIUM) {
            return new RecommendationResponse(RecommendationType.WATCH_RECOVERY,
                    "Есть признаки усталости. Следи за сном и самочувствием; на следующей тренировке "
                            + "не прибавляй вес.");
        }
        return new RecommendationResponse(RecommendationType.KEEP_GOING, "Прогресс идёт — продолжай в том же духе.");
    }

    // ---------- план разгрузки ----------

    /**
     * Неделя разгрузки: обычно её делают раз в 4–6 недель на ~7 дней, снижая объём
     * (меньше подходов) и/или интенсивность. Конкретные 70% и "вдвое меньше подходов" —
     * наш выбор как разумная середина, его можно менять.
     *
     * @param history непустая история упражнения по возрастанию даты
     */
    public static DeloadPlanResponse deloadPlan(List<ExercisePerformance> history, String reason) {
        ExercisePerformance last = history.get(history.size() - 1);
        List<ExercisePerformance> recent = history.subList(Math.max(0, history.size() - 3), history.size());

        int usualSets = median(recent.stream().map(ExercisePerformance::getWorkingSets).toList());
        Integer usualReps = medianOrNull(recent.stream().map(ExercisePerformance::getBestReps).filter(Objects::nonNull).toList());
        int sets = Math.max(1, (int) Math.ceil(usualSets / 2.0));
        int reps = usualReps == null ? DEFAULT_REPS : usualReps;

        BigDecimal currentE1rm = history.stream()
                .map(ExercisePerformance::getBestE1rmKg)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second) // последнее непустое значение
                .orElse(null);
        BigDecimal workingWeight = currentE1rm == null ? null : roundToPlates(
                currentE1rm.multiply(BigDecimal.valueOf(DELOAD_INTENSITY_PERCENT)).divide(HUNDRED, 2, RoundingMode.HALF_UP));

        List<String> notes = new ArrayList<>();
        notes.add("Длительность " + DELOAD_DAYS + " дней — обычный срок разгрузки; её делают раз в 4–6 недель");
        notes.add("Подходов ≈ вдвое меньше: " + sets + " вместо " + usualSets);
        if (workingWeight != null) {
            notes.add("Рабочий вес " + format(workingWeight) + " кг — " + DELOAD_INTENSITY_PERCENT
                    + "% от текущего 1ПМ " + format(currentE1rm) + " кг");
        }
        notes.add("Подходы не до отказа: оставляй 3–4 повтора в запасе");
        notes.add("После разгрузки возвращайся к обычным рабочим весам");

        return new DeloadPlanResponse(last.getExerciseId(), last.getExerciseName(), reason, DELOAD_DAYS,
                DELOAD_INTENSITY_PERCENT, currentE1rm, workingWeight, sets, reps, notes);
    }

    /** Округление до шага блинов: 63.7 -> 62.5, 64.0 -> 65.0. */
    static BigDecimal roundToPlates(BigDecimal weight) {
        return weight.divide(PLATE_STEP_KG, 0, RoundingMode.HALF_UP).multiply(PLATE_STEP_KG);
    }

    private static int median(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        return sorted.get(sorted.size() / 2);
    }

    private static Integer medianOrNull(List<Integer> values) {
        return values.isEmpty() ? null : median(values);
    }

    /** 1.50 -> "1.5", 80.00 -> "80": без лишних нулей. */
    private static String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
