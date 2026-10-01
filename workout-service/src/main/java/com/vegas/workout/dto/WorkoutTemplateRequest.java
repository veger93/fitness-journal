package com.vegas.workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Создание/редактирование комплекса. exerciseIds — в том порядке, в каком упражнения
 * идут в тренировке (экран "Комплекс упражнений" -> "Готово (N)").
 */
public record WorkoutTemplateRequest(

        @NotBlank(message = "название обязательно")
        @Size(max = 100, message = "название не длиннее 100 символов")
        String name,

        @NotNull(message = "цвет обязателен")
        @Pattern(regexp = "^[a-z-]{1,20}$", message = "некорректный ключ цвета")
        String color,

        @Pattern(regexp = "^[a-z0-9-]{1,30}$", message = "некорректный ключ иконки")
        String icon,

        // List<@NotNull UUID>: аннотация на ТИПЕ элемента — проверяется каждый элемент списка
        @NotEmpty(message = "добавьте хотя бы одно упражнение")
        @Size(max = 30, message = "не больше 30 упражнений в комплексе")
        List<@NotNull(message = "id упражнения не может быть пустым") UUID> exerciseIds
) {
}
