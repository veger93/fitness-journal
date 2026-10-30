package com.vegas.workout.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Начать тренировку. Все поля необязательны:
 * {"templateId": "..."} — из комплекса (упражнения и название берутся из него);
 * {"fromWorkoutId": "..."} — повторить прошлую тренировку: те же упражнения в том же порядке, подходы пустые;
 * {} — пустая тренировка, упражнения добавляются по ходу.
 */
public record StartWorkoutRequest(
        UUID templateId,

        UUID fromWorkoutId,

        @Size(max = 100, message = "название не длиннее 100 символов")
        String name
) {
}
