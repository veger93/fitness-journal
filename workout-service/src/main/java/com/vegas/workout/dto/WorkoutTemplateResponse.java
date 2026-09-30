package com.vegas.workout.dto;

import java.util.List;
import java.util.UUID;

/**
 * custom=true -> "Мои комплексы" (плитки), false -> "Готовые комплексы" (чипсы).
 * Количество упражнений ("3 упражнения") фронт берёт как exercises.length.
 */
public record WorkoutTemplateResponse(
        UUID id,
        String name,
        String color,
        String icon,
        boolean custom,
        List<TemplateExerciseResponse> exercises
) {
}
