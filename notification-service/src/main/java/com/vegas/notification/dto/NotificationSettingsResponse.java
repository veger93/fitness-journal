package com.vegas.notification.dto;

import com.vegas.notification.entity.NotificationSettings;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record NotificationSettingsResponse(
        boolean remindersEnabled,
        Set<DayOfWeek> reminderDays,
        LocalTime reminderTime,
        boolean inactivityEnabled,
        int inactivityDays,
        String timeZone
) {
    public static NotificationSettingsResponse from(NotificationSettings s) {
        return new NotificationSettingsResponse(s.isRemindersEnabled(), Set.copyOf(s.getReminderDays()),
                s.getReminderTime(), s.isInactivityEnabled(), s.getInactivityDays(), s.getTimeZone());
    }
}
