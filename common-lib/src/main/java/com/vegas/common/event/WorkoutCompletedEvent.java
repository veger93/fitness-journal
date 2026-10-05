package com.vegas.common.event;

import com.vegas.common.model.BodyPart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * "Тренировка завершена". Публикует workout-service, слушает analytics-service.
 *
 * Событие "толстое" (event-carried state transfer): внутри все упражнения и подходы.
 * Аналитике не нужно ходить в workout-service за подробностями —
 * сервисы не зависят друг от друга по доступности.
 *
 * trackingType — строка, а не enum: enum живёт в workout-service,
 * и контракт между сервисами не должен тянуть за собой его внутренние классы.
 */
public record WorkoutCompletedEvent(
        UUID workoutId,
        UUID userId,
        Instant completedAt,
        List<ExercisePerformed> exercises
) {

    public record ExercisePerformed(
            UUID exerciseId,
            String exerciseName,
            BodyPart bodyPart,
            String trackingType,
            List<SetPerformed> sets
    ) {
    }

    public record SetPerformed(
            BigDecimal weightKg,
            Integer reps,
            Integer durationSec,
            Integer distanceM,
            BigDecimal rpe,
            boolean warmup
    ) {
    }
}
