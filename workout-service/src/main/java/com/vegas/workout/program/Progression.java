package com.vegas.workout.program;

import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.WorkoutSet;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * П10 — двойная прогрессия (docs/program.md): во всех подходах на рабочем весе дошёл до верхней границы повторов
 * два раза подряд — прибавляем шаг веса и начинаем снова с нижней границы. Иначе — тот же вес.
 *
 * «Рабочий вес» — тот, с которым сделано больше всего рабочих подходов (при равенстве — больший), а не самый
 * тяжёлый: 4 подхода по 60 и одна проба на 65 — рабочий вес 60, проба прибавку не включает.
 *
 * Чистый класс без Spring и базы: входы — подходы прошлых тренировок, выход — подсказка. Тесты — ProgressionTest.
 */
public final class Progression {

    /** Шаг прибавки по снаряду — как кнопки ± на фронте (lib/workout.ts WEIGHT_STEP) */
    static final Map<Equipment, BigDecimal> STEP = Map.of(
            Equipment.BARBELL, new BigDecimal("2.5"),
            Equipment.DUMBBELL, new BigDecimal("2.5"),
            Equipment.MACHINE, BigDecimal.ONE,
            Equipment.BODYWEIGHT, new BigDecimal("2.5"));

    public enum Kind {
        /** два раза подряд верхняя граница — прибавить */
        INCREASE,
        /** работать на прежнем весе */
        KEEP,
        /** истории нет (или без веса) — вес подбирает человек */
        NEW
    }

    /**
     * @param baseWeightKg     рабочий вес прошлой тренировки (null — нет истории или упражнение без веса)
     * @param suggestedWeightKg что подставить: base + шаг при INCREASE, иначе base
     * @param lastReps         повторы рабочих подходов на рабочем весе в прошлый раз — для пояснения «было 12, 12, 12»
     */
    public record Hint(Kind kind, BigDecimal baseWeightKg, BigDecimal suggestedWeightKg, List<Integer> lastReps) {
    }

    private Progression() {
    }

    /**
     * @param recent подходы прошлых тренировок с этим упражнением, новые первыми (нужны максимум две)
     */
    public static Hint suggest(List<List<WorkoutSet>> recent, int repsMax, Equipment equipment) {
        Optional<BigDecimal> last = recent.isEmpty() ? Optional.empty() : workingWeight(recent.get(0));
        if (last.isEmpty()) {
            return new Hint(Kind.NEW, null, null, List.of());
        }
        BigDecimal base = last.get();
        List<Integer> lastReps = repsAt(recent.get(0), base);
        boolean twiceAtTop = recent.size() >= 2
                && reachedTop(recent.get(0), base, repsMax)
                && workingWeight(recent.get(1)).map(w -> w.compareTo(base) == 0).orElse(false)
                && reachedTop(recent.get(1), base, repsMax);
        if (twiceAtTop) {
            return new Hint(Kind.INCREASE, base, base.add(STEP.getOrDefault(equipment, BigDecimal.ONE)), lastReps);
        }
        return new Hint(Kind.KEEP, base, base, lastReps);
    }

    /**
     * Вес, с которым сделано больше всего рабочих подходов; при равенстве — больший («лесенка» 20, 30, 40
     * по одному подходу — рабочий 40, как и в подсказке подхода на фронте: lib/workout.ts workingSet)
     */
    static Optional<BigDecimal> workingWeight(List<WorkoutSet> sets) {
        return working(sets).stream()
                .collect(Collectors.groupingBy(s -> s.getWeightKg().stripTrailingZeros(), Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.<Map.Entry<BigDecimal, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey);
    }

    /** Все рабочие подходы на этом весе — не меньше верхней границы повторов */
    static boolean reachedTop(List<WorkoutSet> sets, BigDecimal weight, int repsMax) {
        List<Integer> reps = repsAt(sets, weight);
        return !reps.isEmpty() && reps.stream().allMatch(r -> r >= repsMax);
    }

    private static List<Integer> repsAt(List<WorkoutSet> sets, BigDecimal weight) {
        return working(sets).stream()
                .filter(s -> s.getWeightKg().compareTo(weight) == 0)
                .map(WorkoutSet::getReps)
                .toList();
    }

    /** Рабочие подходы с весом и повторами (разминку не считаем; подходы без веса — не для прогрессии по весу) */
    private static List<WorkoutSet> working(List<WorkoutSet> sets) {
        return sets.stream()
                .filter(s -> !s.isWarmup() && s.getWeightKg() != null && s.getReps() != null)
                .toList();
    }
}
