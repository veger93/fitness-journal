package com.vegas.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Удаление уведомлений, настроек и адреса пользователя (событие UserDeleted). Устроено как в workout-service */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataCleaner {

    private final JdbcTemplate jdbc;

    @Transactional
    public void deleteAll(UUID userId) {
        int notifications = jdbc.update("DELETE FROM notifications WHERE user_id = ?", userId);
        // уведомления тренера об этом клиенте: открыть его дневник уже нельзя
        jdbc.update("DELETE FROM notifications WHERE client_id = ?", userId);
        // копия связей: удалили клиента — его связь; удалили тренера — связи со всеми его клиентами
        jdbc.update("DELETE FROM trainer_links WHERE client_id = ? OR trainer_id = ?", userId, userId);
        jdbc.update("DELETE FROM notification_settings WHERE user_id = ?", userId);
        // дни плана удалятся сами (ON DELETE CASCADE от weekly_plans)
        jdbc.update("DELETE FROM weekly_plans WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM workout_activity WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM recipients WHERE user_id = ?", userId);
        log.info("User {} notifications deleted: {}", userId, notifications);
    }
}
