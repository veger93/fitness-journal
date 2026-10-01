package com.vegas.workout.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddWorkoutExerciseRequest(
        @NotNull(message = "exerciseId обязателен")
        UUID exerciseId
) {
}
