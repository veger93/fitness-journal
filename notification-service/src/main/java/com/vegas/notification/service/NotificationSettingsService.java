package com.vegas.notification.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.notification.dto.NotificationSettingsRequest;
import com.vegas.notification.dto.NotificationSettingsResponse;
import com.vegas.notification.entity.NotificationSettings;
import com.vegas.notification.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository repository;

    /** Строки в БД может не быть (пользователь ничего не настраивал) — тогда отдаём значения по умолчанию, не сохраняя. */
    @Transactional(readOnly = true)
    public NotificationSettingsResponse get(UUID userId) {
        return NotificationSettingsResponse.from(
                repository.findById(userId).orElseGet(() -> NotificationSettings.defaults(userId)));
    }

    @Transactional
    public NotificationSettingsResponse update(UUID userId, NotificationSettingsRequest request) {
        validate(request);
        NotificationSettings settings = repository.findById(userId)
                .orElseGet(() -> repository.save(NotificationSettings.defaults(userId)));
        settings.update(request.remindersEnabled(), request.reminderDays(), request.reminderTime(),
                request.inactivityEnabled(), request.inactivityDays(), request.timeZone());
        return NotificationSettingsResponse.from(settings);
    }

    static void validate(NotificationSettingsRequest request) {
        if (request.remindersEnabled()
                && (request.reminderDays() == null || request.reminderDays().isEmpty() || request.reminderTime() == null)) {
            throw new BadRequestException("Для напоминаний нужно выбрать дни недели и время");
        }
        try {
            ZoneId.of(request.timeZone());
        } catch (DateTimeException e) {
            throw new BadRequestException("Неизвестный часовой пояс: " + request.timeZone());
        }
    }
}
