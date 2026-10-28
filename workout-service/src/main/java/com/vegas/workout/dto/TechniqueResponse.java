package com.vegas.workout.dto;

import java.util.List;

/** Техника выполнения для карточки упражнения. */
public record TechniqueResponse(
        String setup,           // исходное положение
        List<String> steps,     // как выполнять
        List<String> mistakes   // частые ошибки
) {
}
