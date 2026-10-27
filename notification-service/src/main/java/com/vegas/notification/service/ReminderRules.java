package com.vegas.notification.service;

import com.vegas.notification.entity.NotificationSettings;
import com.vegas.notification.entity.ReminderMode;
import com.vegas.notification.entity.WeeklyPlan;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

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
     * В какие дни напоминать. PLAN — дни плана на неделю (нет плана — нет напоминаний: напоминать не о чем),
     * CUSTOM — дни, выбранные вручную.
     */
    public static Set<DayOfWeek> reminderDays(NotificationSettings settings, Optional<WeeklyPlan> plan) {
        if (settings.getReminderMode() == ReminderMode.CUSTOM) {
            return settings.getReminderDays();
        }
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        plan.ifPresent(p -> p.getDays().forEach(day -> days.add(day.dayOfWeek())));
        return days;
    }

    /**
     * Напоминание по расписанию.
     *
     * @param days дни напоминаний — результат {@link #reminderDays}
     * @return локальная дата тренировки, о которой пора напомнить (она же — часть ключа дублей),
     *         или empty, если сейчас напоминать не нужно
     */
    public static Optional<LocalDate> scheduledReminderDue(NotificationSettings settings, Set<DayOfWeek> days,
                                                           Instant now, Optional<Instant> lastWorkoutAt) {
        if (!settings.isRemindersEnabled() || settings.getReminderTime() == null) {
            return Optional.empty();
        }
        ZoneId zone = ZoneId.of(settings.getTimeZone());
        LocalDate today = now.atZone(zone).toLocalDate();

        // проверяем и вчера: напоминание на 23:30 + окно 2 часа заканчивается уже завтра
        for (LocalDate day : new LocalDate[]{today, today.minusDays(1)}) {
            if (!days.contains(day.getDayOfWeek())) {
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
     * @return сколько календарных дней прошло с последней тренировки, если пора напомнить; иначе empty.
     *         Пользователю, который ещё ни разу не тренировался, не напоминаем — нечего "продолжать".
     */
    public static Optional<Long> inactivityDue(NotificationSettings settings, Instant now, Optional<Instant> lastWorkoutAt) {
        if (!settings.isInactivityEnabled() || lastWorkoutAt.isEmpty()) {
            return Optional.empty();
        }
        ZoneId zone = ZoneId.of(settings.getTimeZone());
        LocalTime localTime = now.atZone(zone).toLocalTime();
        if (localTime.isBefore(DAY_START) || !localTime.isBefore(DAY_END)) {
            return Optional.empty();
        }
        // календарные дни в поясе пользователя, а не полные сутки: тренировка в субботу 23:00,
        // сейчас среда 09:00 — для человека это "4 дня назад", хотя полных суток прошло 3
        long days = ChronoUnit.DAYS.between(lastWorkoutAt.get().atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate());
        return days >= settings.getInactivityDays() ? Optional.of(days) : Optional.empty();
    }
}
