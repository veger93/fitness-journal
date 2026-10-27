package com.vegas.notification.scheduler;

import com.vegas.notification.entity.NotificationSettings;
import com.vegas.notification.entity.NotificationType;
import com.vegas.notification.entity.ReminderMode;
import com.vegas.notification.repository.NotificationSettingsRepository;
import com.vegas.notification.service.NotificationService;
import com.vegas.notification.service.WeeklyPlanSyncService;
import com.vegas.notification.service.WorkoutActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    // понедельник, 19:00:30 UTC
    private static final Instant NOW = Instant.parse("2026-10-19T19:00:30Z");

    @Mock
    private NotificationSettingsRepository settingsRepository;
    @Mock
    private WorkoutActivityService activityService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WeeklyPlanSyncService planService;

    private ReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReminderScheduler(settingsRepository, activityService, planService, notificationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void scheduledReminder_usesDayAsDedupeKey() {
        NotificationSettings settings = reminderAt19();
        when(settingsRepository.findByRemindersEnabledTrue()).thenReturn(List.of(settings));
        when(activityService.lastWorkoutAt(settings.getUserId())).thenReturn(Optional.empty());

        scheduler.sendScheduledReminders();

        verify(notificationService).send(eq(settings.getUserId()), eq(NotificationType.WORKOUT_REMINDER),
                eq("REMINDER:2026-10-19T19:00"), any());
    }

    @Test
    void inactivity_usesLastWorkoutAsDedupeKey() {
        NotificationSettings settings = NotificationSettings.defaults(UUID.randomUUID());
        settings.update(false, ReminderMode.PLAN, null, null, true, 7, "UTC");
        Instant last = Instant.parse("2026-10-05T18:00:00Z");
        when(settingsRepository.findByInactivityEnabledTrue()).thenReturn(List.of(settings));
        when(activityService.lastWorkoutAt(settings.getUserId())).thenReturn(Optional.of(last));

        scheduler.sendInactivityReminders();

        verify(notificationService).send(eq(settings.getUserId()), eq(NotificationType.INACTIVITY),
                eq("INACTIVITY:2026-10-05T18:00:00Z"), any());
    }

    @Test
    void failureForOneUser_doesNotStopOthers() {
        NotificationSettings first = reminderAt19();
        NotificationSettings second = reminderAt19();
        when(settingsRepository.findByRemindersEnabledTrue()).thenReturn(List.of(first, second));
        when(activityService.lastWorkoutAt(any())).thenReturn(Optional.empty());
        // первый вызов (первый пользователь) падает, второй проходит
        doThrow(new IllegalStateException("SMTP down")).doReturn(Optional.empty()).when(notificationService)
                .send(any(), any(), any(), any());

        scheduler.sendScheduledReminders();

        verify(notificationService).send(eq(second.getUserId()), eq(NotificationType.WORKOUT_REMINDER), any(), any());
    }

    @Test
    void trainedToday_noReminder() {
        NotificationSettings settings = reminderAt19();
        when(settingsRepository.findByRemindersEnabledTrue()).thenReturn(List.of(settings));
        when(activityService.lastWorkoutAt(settings.getUserId()))
                .thenReturn(Optional.of(Instant.parse("2026-10-19T08:00:00Z")));

        scheduler.sendScheduledReminders();

        verify(notificationService, never()).send(any(), any(), any(), any());
    }

    private static NotificationSettings reminderAt19() {
        NotificationSettings settings = NotificationSettings.defaults(UUID.randomUUID());
        settings.update(true, ReminderMode.CUSTOM, Set.of(DayOfWeek.MONDAY), LocalTime.of(19, 0), false, 7, "UTC");
        return settings;
    }
}
