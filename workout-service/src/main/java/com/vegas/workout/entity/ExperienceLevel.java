package com.vegas.workout.entity;

/**
 * Опыт тренировок. Своя копия enum из user-service: сервисы не делят код сущностей,
 * значения совпадают по имени — так они и ходят между сервисами (строкой в JSON и в БД).
 */
public enum ExperienceLevel {
    BEGINNER,      // до 6 месяцев
    INTERMEDIATE,  // 6 месяцев – 2 года
    ADVANCED       // больше 2 лет
}
