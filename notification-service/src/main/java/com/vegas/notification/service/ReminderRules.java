package com.vegas.notification.service;

import com.vegas.notification.entity.NotificationSettings;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Когда пора напоминать. Чистые функции без Spring и БД: всё, что нужно, передаётся параметрами,
 * включая "сейчас" — поэтому правила легко проверить тестами на любой момент времени.
 *
 * Время пользователя всегда считаем в ЕГО часовом поясе: "19:00" в Екатеринбурге — это 14:00 UTC.
 */
public final class ReminderRules {

    /**
     * Сколько после назначенного времени напоминание ещё актуально. Планировщик срабатывает раз в минуту,
     * но сервис мог быть выключен ровно в 19:00 — тогда отправим в 19:05. А в 23:00 уже нет: поздно.
     */
    static final Duration REMINDER_WINDOW = Duration.ofHours(2);

    /** "Давно не было тренировок" не шлём ночью: только с 9:00 до 21:00 по времени пользователя. */
    static final LocalTime DAY_START = LocalTime.of(9, 0);
    static final LocalTime DAY_END = LocalTime.of(21, 0);

    private ReminderRules() {
    }

    /**
     * Напоминание по расписанию.
     *
     * @return локальная дата тренировки, о которой пора напомнить (она же — часть ключа дублей),
     *         или empty, если сейчас напоминать не нужно
     */
    public static Optional<LocalDate> scheduledReminderDue(NotificationSettings settings, Instant now,
                                                           Optional<Instant> lastWorkoutAt) {
        if (!settings.isRemindersEnabled() || settings.getReminderTime() == null) {
            return Optional.empty();
        }
        ZoneId zone = ZoneId.of(settings.getTimeZone());
        LocalDate today = now.atZone(zone).toLocalDate();

        // проверяем и вчера: напоминание на 23:30 + окно 2 часа заканчивается уже завтра
        for (LocalDate day : new LocalDate[]{today, today.minusDays(1)}) {
            if (!settings.getReminderDays().contains(day.getDayOfWeek())) {
                continue;
            }
            // ZonedDateTime.of сам разруливает переход на летнее время (несуществующее 02:30 сдвинет на 03:30)
            Instant remindAt = ZonedDateTime.of(day, settings.getReminderTime(), zone).toInstant();
            if (now.isBefore(remindAt) || !now.isBefore(remindAt.plus(REMINDER_WINDOW))) {
                continue;
            }
            Instant dayStart = day.atStartOfDay(zone).toInstant();
            boolean alreadyTrained = lastWorkoutAt.map(last -> !last.isBefore(dayStart)).orElse(false);
            return alreadyTrained ? Optional.empty() : Optional.of(day);
        }
        return Optional.empty();
    }

    /**
     * "Давно не было тренировок".
     *
     * @return сколько полных дней прошло с последней тренировки, если пора напомнить; иначе empty.
     *         Пользователю, который ещё ни разу не тренировался, не напоминаем — нечего "продолжать".
     */
    public static Optional<Long> inactivityDue(NotificationSettings settings, Instant now, Optional<Instant> lastWorkoutAt) {
        if (!settings.isInactivityEnabled() || lastWorkoutAt.isEmpty()) {
            return Optional.empty();
        }
        LocalTime localTime = now.atZone(ZoneId.of(settings.getTimeZone())).toLocalTime();
        if (localTime.isBefore(DAY_START) || !localTime.isBefore(DAY_END)) {
            return Optional.empty();
        }
        long days = Duration.between(lastWorkoutAt.get(), now).toDays();
        return days >= settings.getInactivityDays() ? Optional.of(days) : Optional.empty();
    }
}
