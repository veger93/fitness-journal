package com.vegas.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Событие "тренировка завершена". Публикует workout-service,
 * слушает analytics-service (пересчёт прогресса, плато, рекордов).
 * Пока Kafka не подключена — класс просто лежит как контракт между сервисами.
 */
public record WorkoutCompletedEvent(
        UUID workoutId,
        UUID userId,
        Instant completedAt
) {
}
