package com.vegas.workout.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Замена упражнения в программе. excludeOld — галочка «Больше не предлагать» старое упражнение.
 * {"exerciseId": "...", "excludeOld": true}
 */
public record ProgramSwapRequest(
        @NotNull(message = "exerciseId обязателен") UUID exerciseId,
        boolean excludeOld
) {
}
