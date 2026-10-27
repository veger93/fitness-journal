package com.vegas.notification.service;

import com.vegas.notification.dto.NotificationResponse;
import com.vegas.notification.entity.NotificationSettings;
import com.vegas.notification.entity.NotificationType;
import com.vegas.notification.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * "Проверить напоминание" из профиля: прислать письмо-напоминание прямо сейчас,
 * без расписания и без правил "уже тренировался сегодня". Так пользователь видит, что вся цепочка работает:
 * адрес почты известен, почтовый сервер доступен, письмо собирается.
 */
@Service
@RequiredArgsConstructor
public class TestReminderService {

    private final NotificationSettingsRepository settingsRepository;
    private final WeeklyPlanSyncService planService;
    private final NotificationService notificationService;
    private final Clock clock;

    public NotificationResponse send(UUID userId) {
        Instant now = clock.instant();
        String timeZone = settingsRepository.findById(userId).map(NotificationSettings::getTimeZone).orElse("UTC");
        LocalDate today = now.atZone(ZoneId.of(timeZone)).toLocalDate();
        Optional<String> template = planService.find(userId).flatMap(plan -> plan.templateFor(today.getDayOfWeek()));

        // ключ с текущим моментом — каждая проверка уникальна и не мешает настоящим напоминаниям
        return notificationService.send(userId, NotificationType.WORKOUT_REMINDER, "TEST:" + now,
                        greeting -> NotificationTemplates.workoutReminder(greeting, today, template))
                .map(NotificationResponse::from)
                .orElseThrow(() -> new IllegalStateException("Test reminder with unique key was treated as duplicate"));
    }
}
