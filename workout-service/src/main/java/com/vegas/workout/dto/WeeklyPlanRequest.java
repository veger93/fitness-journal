package com.vegas.workout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

/**
 * Новый план целиком (PUT заменяет старый). Дни, которых нет в списке или с templateId = null, — отдых.
 * {"days": [{"dayOfWeek": "MONDAY", "templateId": "..."}, {"dayOfWeek": "WEDNESDAY", "templateId": "..."}]}
 */
public record WeeklyPlanRequest(
        @NotNull(message = "days обязателен")
        @Size(max = 7, message = "в неделе 7 дней")
        List<@Valid @NotNull Day> days
) {
    public record Day(
            @NotNull(message = "dayOfWeek обязателен")
            DayOfWeek dayOfWeek,
            UUID templateId
    ) {
    }
}
