package com.vegas.workout.dto;

import com.vegas.workout.entity.DeloadPeriod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Активная разгрузка: что подсказывать в подходе и до какого момента. */
public record DeloadResponse(
        UUID exerciseId,
        BigDecimal workingWeightKg,
        int sets,
        int reps,
        Instant startedAt,
        Instant endsAt
) {
    public static DeloadResponse from(DeloadPeriod period) {
        return new DeloadResponse(period.getExerciseId(), period.getWorkingWeightKg(), period.getSets(),
                period.getReps(), period.getStartedAt(), period.getEndsAt());
    }
}
