package com.vegas.workout.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Удаление всех данных пользователя (событие UserDeleted): тренер удалил клиента без аккаунта.
 *
 * Обычный SQL через JdbcTemplate, а не репозитории: это массовое удаление по user_id, сущности в память грузить незачем.
 * Порядок важен из-за внешних ключей: сначала то, что ссылается, потом то, на что ссылаются.
 * Подходы и упражнения тренировок удалятся сами (ON DELETE CASCADE от workouts).
 * Повтор события безопасен: удалять уже нечего.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataCleaner {

    private final JdbcTemplate jdbc;

    @Transactional
    public void deleteAll(UUID userId) {
        int plan = jdbc.update("DELETE FROM weekly_plan_days WHERE user_id = ?", userId);
        int deloads = jdbc.update("DELETE FROM deload_periods WHERE user_id = ?", userId);
        int workouts = jdbc.update("DELETE FROM workouts WHERE user_id = ?", userId);
        int templates = jdbc.update("DELETE FROM workout_templates WHERE owner_id = ?", userId);
        // свои упражнения — после тренировок и комплексов, которые на них ссылаются
        int exercises = jdbc.update("DELETE FROM exercises WHERE owner_id = ?", userId);
        jdbc.update("DELETE FROM trainer_links WHERE client_id = ?", userId);
        log.info("User {} data deleted: {} workouts, {} templates, {} exercises, {} plan days, {} deloads",
                userId, workouts, templates, exercises, plan, deloads);
    }
}
