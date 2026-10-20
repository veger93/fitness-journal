package com.vegas.workout.dto;

import com.vegas.workout.entity.WorkoutStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Полная тренировка: экран тренировки и экран завершения (длительность, подходы, тоннаж).
 * startedAt = null — тренировка подготовлена, но ещё не начата (таймер не идёт).
 */
public record WorkoutResponse(
        UUID id,
        String name,
        UUID templateId,
        WorkoutStatus status,
        Instant startedAt,
        Instant completedAt,
        Long durationSec,
        long workingSets,
        BigDecimal tonnageKg,
        List<WorkoutExerciseResponse> exercises
) {
}
