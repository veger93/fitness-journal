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
        List<TemplateExerciseResponse> exercises,
        boolean program
) {
    /**
     * program = день активной программы (раздел «Программа»; удалить и поменять — только через программу).
     * static, а не метод экземпляра: MapStruct принял бы withX/одноаргументный метод за «свойство» и ругался
     */
    public static WorkoutTemplateResponse programDay(WorkoutTemplateResponse t, boolean program) {
        return new WorkoutTemplateResponse(t.id(), t.name(), t.color(), t.icon(), t.custom(), t.exercises(), program);
    }
}
