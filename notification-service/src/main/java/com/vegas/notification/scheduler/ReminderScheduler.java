package com.vegas.notification.scheduler;

import com.vegas.notification.entity.NotificationSettings;
import com.vegas.notification.entity.NotificationType;
import com.vegas.notification.entity.WeeklyPlan;
import com.vegas.notification.repository.NotificationSettingsRepository;
import com.vegas.notification.service.NotificationService;
import com.vegas.notification.service.NotificationTemplates;
import com.vegas.notification.service.ReminderRules;
import com.vegas.notification.service.WeeklyPlanSyncService;
import com.vegas.notification.service.WorkoutActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

/**
 * Два планировщика напоминаний. Решение "пора или нет" — в ReminderRules, здесь только обход пользователей.
 *
 * Дубли: каждую минуту одно и то же напоминание "пора" — отправит его только первый запуск,
 * дальше NotificationService увидит ключ в журнале. Ключи:
 *   REMINDER:2026-10-19             — одно напоминание на день тренировки;
 *   INACTIVITY:<дата последней>     — одно на перерыв: потренировался — дата сменилась — новый перерыв.
 *
 * Ограничения MVP (осознанные):
 * - обходим всех пользователей с включёнными напоминаниями каждую минуту. Для тысяч — ок,
 *   для миллионов нужно хранить next_reminder_at и выбирать только "созревших";
 * - при двух экземплярах сервиса оба запустят планировщик. UNIQUE в журнале не даст записать дубль,
 *   но письмо могут успеть отправить оба. Решение — ShedLock (блокировка задачи через БД), добавим при масштабировании.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final NotificationSettingsRepository settingsRepository;
    private final WorkoutActivityService activityService;
    private final WeeklyPlanSyncService planService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "${app.reminders.schedule-cron}", zone = "UTC")
    public void sendScheduledReminders() {
        Instant now = clock.instant();
        for (NotificationSettings settings : settingsRepository.findByRemindersEnabledTrue()) {
            safely(settings, () -> {
                Optional<Instant> last = activityService.lastWorkoutAt(settings.getUserId());
                Optional<WeeklyPlan> plan = planService.find(settings.getUserId());
                Set<DayOfWeek> days = ReminderRules.reminderDays(settings, plan);
                ReminderRules.scheduledReminderDue(settings, days, now, last).ifPresent(day ->
                        notificationService.send(settings.getUserId(), NotificationType.WORKOUT_REMINDER,
                                "REMINDER:" + day,
                                greeting -> NotificationTemplates.workoutReminder(greeting, day,
                                        plan.flatMap(p -> p.templateFor(day.getDayOfWeek())))));
            });
        }
    }

    @Scheduled(cron = "${app.reminders.inactivity-cron}", zone = "UTC")
    public void sendInactivityReminders() {
        Instant now = clock.instant();
        for (NotificationSettings settings : settingsRepository.findByInactivityEnabledTrue()) {
            safely(settings, () -> {
                Optional<Instant> last = activityService.lastWorkoutAt(settings.getUserId());
                ReminderRules.inactivityDue(settings, now, last).ifPresent(days ->
                        notificationService.send(settings.getUserId(), NotificationType.INACTIVITY,
                                "INACTIVITY:" + last.get(),
                                greeting -> NotificationTemplates.inactivity(greeting, days,
                                        last.get().atZone(ZoneId.of(settings.getTimeZone())).toLocalDate())));
            });
        }
    }

    /** Ошибка у одного пользователя (например, почта недоступна) не должна остановить обход остальных. */
    private void safely(NotificationSettings settings, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Reminder for user {} failed", settings.getUserId(), e);
        }
    }
}
