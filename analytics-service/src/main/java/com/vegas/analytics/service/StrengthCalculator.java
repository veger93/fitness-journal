package com.vegas.analytics.service;

import com.vegas.analytics.entity.ExercisePerformance;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * "Сила" упражнения — то, что показываем крупной цифрой (главная, страница упражнения).
 *
 * Раньше это был 1ПМ последней тренировки: лёгкий день (15 × 10 после 27,5 × 10) "ронял" силу с 36,7 до 20 кг,
 * хотя за день сила никуда не девается. Теперь — лучший расчётный 1ПМ за 4 недели:
 * по мета-анализу Bosquet et al. (2013) максимальная сила почти не снижается первые ~4 недели без тренировок,
 * дальше спад ускоряется (docs/analytics.md).
 *
 * Упражнение не делали дольше 4 недель — показываем силу на момент последней тренировки (лучшее за 4 недели до неё)
 * с пометкой stale: сколько именно сила упала, мы не знаем, поэтому не придумываем число, а честно говорим "давно".
 */
final class StrengthCalculator {

    static final Duration WINDOW = Duration.ofDays(28);

    /** kg — сила (null, если у упражнения нет 1ПМ: бег, планка, подтягивания без веса); stale — давно не делали */
    record Strength(BigDecimal kg, boolean stale) {
    }

    private StrengthCalculator() {
    }

    /** history — тренировки одного упражнения, от старых к новым */
    static Strength of(List<ExercisePerformance> history, Instant now) {
        List<ExercisePerformance> withE1rm = history.stream().filter(p -> p.getBestE1rmKg() != null).toList();
        if (withE1rm.isEmpty()) {
            return new Strength(null, false);
        }
        Instant last = withE1rm.get(withE1rm.size() - 1).getPerformedAt();
        boolean stale = Duration.between(last, now).compareTo(WINDOW) > 0;
        Instant from = (stale ? last : now).minus(WINDOW);
        BigDecimal best = withE1rm.stream()
                .filter(p -> !p.getPerformedAt().isBefore(from))
                .map(ExercisePerformance::getBestE1rmKg)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new Strength(best, stale);
    }
}
