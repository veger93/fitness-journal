package com.vegas.analytics.service;

import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutCompletedEvent.ExercisePerformed;
import com.vegas.common.event.WorkoutCompletedEvent.SetPerformed;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Чистая функция: подходы упражнения -> итоговые цифры. Без БД и Spring — легко тестировать.
 * Разминочные подходы не учитываются нигде.
 */
public final class PerformanceCalculator {

    private PerformanceCalculator() {
    }

    /** Empty — если рабочих подходов не было (только разминка): такая тренировка прогресс не двигает. */
    public static Optional<ExercisePerformance> calculate(WorkoutCompletedEvent event, ExercisePerformed exercise) {
        List<SetPerformed> working = exercise.sets().stream().filter(set -> !set.warmup()).toList();
        if (working.isEmpty()) {
            return Optional.empty();
        }

        // лучший подход = с максимальным расчётным 1ПМ (только подходы с весом и повторами)
        Optional<SetPerformed> bestWeighted = working.stream()
                .filter(PerformanceCalculator::isWeighted)
                .max(Comparator.comparing(set -> OneRepMax.epley(set.weightKg(), set.reps())));

        return Optional.of(ExercisePerformance.builder()
                .userId(event.userId())
                .workoutId(event.workoutId())
                .exerciseId(exercise.exerciseId())
                .exerciseName(exercise.exerciseName())
                .bodyPart(exercise.bodyPart())
                .trackingType(exercise.trackingType())
                .performedAt(event.completedAt())
                .workingSets(working.size())
                .totalReps(sumReps(working))
                .volumeKg(volume(working))
                .bestWeightKg(bestWeighted.map(SetPerformed::weightKg).orElse(null))
                // для упражнений без веса (подтягивания без отягощения) лучший = максимум повторов
                .bestReps(bestWeighted.map(SetPerformed::reps).orElseGet(() -> maxOf(working, SetPerformed::reps)))
                .bestE1rmKg(bestWeighted.map(set -> OneRepMax.epley(set.weightKg(), set.reps())).orElse(null))
                .bestDurationSec(maxOf(working, SetPerformed::durationSec))
                .bestDistanceM(maxOf(working, SetPerformed::distanceM))
                .build());
    }

    private static boolean isWeighted(SetPerformed set) {
        return set.weightKg() != null && set.weightKg().signum() > 0 && set.reps() != null;
    }

    private static BigDecimal volume(List<SetPerformed> sets) {
        return sets.stream()
                .filter(PerformanceCalculator::isWeighted)
                .map(set -> set.weightKg().multiply(BigDecimal.valueOf(set.reps())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Integer sumReps(List<SetPerformed> sets) {
        List<Integer> reps = sets.stream().map(SetPerformed::reps).filter(Objects::nonNull).toList();
        return reps.isEmpty() ? null : reps.stream().mapToInt(Integer::intValue).sum();
    }

    private static Integer maxOf(List<SetPerformed> sets, java.util.function.Function<SetPerformed, Integer> field) {
        return sets.stream().map(field).filter(Objects::nonNull).max(Integer::compare).orElse(null);
    }
}
