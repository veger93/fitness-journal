package com.vegas.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Удаление всей аналитики пользователя (событие UserDeleted). Устроено как в workout-service */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataCleaner {

    private final JdbcTemplate jdbc;

    @Transactional
    public void deleteAll(UUID userId) {
        int performances = jdbc.update("DELETE FROM exercise_performances WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM athlete_profiles WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM trainer_links WHERE client_id = ?", userId);
        log.info("User {} analytics deleted: {} performances", userId, performances);
    }
}
