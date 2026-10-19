package com.vegas.notification.service;

import com.vegas.notification.entity.NotificationSettings;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 2026-10-19 — понедельник. Екатеринбург = UTC+5: 19:00 там — 14:00 UTC. */
class ReminderRulesTest {

    private static final LocalDate MONDAY = LocalDate.parse("2026-10-19");

    @Test
    void reminder_dueAtLocalTime_inUserZone() {
        NotificationSettings s = reminders(LocalTime.of(19, 0), "Asia/Yekaterinburg");

        assertThat(ReminderRules.scheduledReminderDue(s, at("2026-10-19T13:59:00Z"), Optional.empty())).isEmpty();
        assertThat(ReminderRules.scheduledReminderDue(s, at("2026-10-19T14:00:30Z"), Optional.empty())).contains(MONDAY);
    }

    @Test
    void reminder_notSentAfterWindow() {
        NotificationSettings s = reminders(LocalTime.of(19, 0), "Asia/Yekaterinburg");

        assertThat(ReminderRules.scheduledReminderDue(s, at("2026-10-19T15:59:00Z"), Optional.empty())).contains(MONDAY);
        assertThat(ReminderRules.scheduledReminderDue(s, at("2026-10-19T16:00:00Z"), Optional.empty())).isEmpty();
    }

    @Test
    void reminder_onlyOnSelectedDays() {
        NotificationSettings s = reminders(LocalTime.of(19, 0), "Asia/Yekaterinburg");

        // вторник
        assertThat(ReminderRules.scheduledReminderDue(s, at("2026-10-20T14:00:00Z"), Optional.empty())).isEmpty();
    }

    @Test
    void reminder_skippedIfAlreadyTrainedToday() {
        NotificationSettings s = reminders(LocalTime.of(19, 0), "Asia/Yekaterinburg");
        Instant now = at("2026-10-19T14:01:00Z");

        // 10:00 по Екатеринбургу того же дня
        assertThat(ReminderRules.scheduledReminderDue(s, now, Optional.of(at("2026-10-19T05:00:00Z")))).isEmpty();
        // вчера — напоминаем
        assertThat(ReminderRules.scheduledReminderDue(s, now, Optional.of(at("2026-10-18T12:00:00Z")))).contains(MONDAY);
    }

    @Test
    void reminder_lateEvening_windowCrossesMidnight() {
        NotificationSettings s = reminders(LocalTime.of(23, 30), "UTC");

        // уже вторник 00:30, но это напоминание на понедельник 23:30
        assertThat(ReminderRules.scheduledReminderDue(s, at("2026-10-20T00:30:00Z"), Optional.empty())).contains(MONDAY);
    }

    @Test
    void inactivity_dueAfterConfiguredDays() {
        NotificationSettings s = inactivity(7);
        Optional<Instant> last = Optional.of(at("2026-10-10T18:00:00Z"));

        // 10 -> 19 октября = 9 календарных дней (хотя полных суток — 8)
        assertThat(ReminderRules.inactivityDue(s, at("2026-10-19T12:00:00Z"), last)).contains(9L);
        // 10 -> 16 октября = 6 дней < 7
        assertThat(ReminderRules.inactivityDue(s, at("2026-10-16T12:00:00Z"), last)).isEmpty();
        // ровно 7 — уже пора
        assertThat(ReminderRules.inactivityDue(s, at("2026-10-17T10:00:00Z"), last)).contains(7L);
    }

    @Test
    void inactivity_notAtNight() {
        NotificationSettings s = inactivity(7);
        Optional<Instant> last = Optional.of(at("2026-10-10T18:00:00Z"));

        assertThat(ReminderRules.inactivityDue(s, at("2026-10-19T22:00:00Z"), last)).isEmpty();
        assertThat(ReminderRules.inactivityDue(s, at("2026-10-19T08:59:00Z"), last)).isEmpty();
    }

    @Test
    void inactivity_neverTrained_noReminder() {
        assertThat(ReminderRules.inactivityDue(inactivity(3), at("2026-10-19T12:00:00Z"), Optional.empty())).isEmpty();
    }

    @Test
    void disabled_noReminders() {
        NotificationSettings s = NotificationSettings.defaults(UUID.randomUUID());
        Optional<Instant> last = Optional.of(at("2026-01-01T00:00:00Z"));

        assertThat(ReminderRules.scheduledReminderDue(s, at("2026-10-19T14:00:00Z"), last)).isEmpty();
        assertThat(ReminderRules.inactivityDue(s, at("2026-10-19T14:00:00Z"), last)).isEmpty();
    }

    static NotificationSettings reminders(LocalTime time, String zone) {
        NotificationSettings s = NotificationSettings.defaults(UUID.randomUUID());
        s.update(true, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), time, false, 7, zone);
        return s;
    }

    static NotificationSettings inactivity(int days) {
        NotificationSettings s = NotificationSettings.defaults(UUID.randomUUID());
        s.update(false, null, null, true, days, "UTC");
        return s;
    }

    private static Instant at(String iso) {
        return Instant.parse(iso);
    }
}
