package com.vegas.workout.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Начать тренировку. Оба поля необязательны:
 * {"templateId": "..."} — из комплекса (упражнения и название берутся из него);
 * {} — пустая тренировка, упражнения добавляются по ходу.
 */
public record StartWorkoutRequest(
        UUID templateId,

        @Size(max = 100, message = "название не длиннее 100 символов")
        String name
) {
}
