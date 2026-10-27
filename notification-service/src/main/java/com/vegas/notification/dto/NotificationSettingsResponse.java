package com.vegas.notification.dto;

import com.vegas.notification.entity.NotificationSettings;
import com.vegas.notification.entity.ReminderMode;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record NotificationSettingsResponse(
        boolean remindersEnabled,
        ReminderMode reminderMode,
        Set<DayOfWeek> reminderDays,
        LocalTime reminderTime,
        boolean inactivityEnabled,
        int inactivityDays,
        String timeZone
) {
    public static NotificationSettingsResponse from(NotificationSettings s) {
        return new NotificationSettingsResponse(s.isRemindersEnabled(), s.getReminderMode(), Set.copyOf(s.getReminderDays()),
                s.getReminderTime(), s.isInactivityEnabled(), s.getInactivityDays(), s.getTimeZone());
    }
}
