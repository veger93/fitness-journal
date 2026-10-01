package com.vegas.common.event;

import java.util.UUID;

/**
 * "Завершённая тренировка удалена" — аналитика должна выкинуть её данные из прогресса и рекордов.
 */
public record WorkoutDeletedEvent(
        UUID workoutId,
        UUID userId
) {
}
