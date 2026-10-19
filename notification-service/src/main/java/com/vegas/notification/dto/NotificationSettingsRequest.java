package com.vegas.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * Настройки целиком (PUT заменяет всё). Простые проверки — аннотациями,
 * проверки "поле зависит от поля" — в NotificationSettingsService.
 *
 * reminderTime — локальное время пользователя ("19:00"), timeZone — IANA, например "Europe/Moscow".
 */
public record NotificationSettingsRequest(
        @NotNull Boolean remindersEnabled,
        Set<DayOfWeek> reminderDays,
        LocalTime reminderTime,
        @NotNull Boolean inactivityEnabled,
        @NotNull @Min(3) @Max(30) Integer inactivityDays,
        @NotBlank String timeZone
) {
}
