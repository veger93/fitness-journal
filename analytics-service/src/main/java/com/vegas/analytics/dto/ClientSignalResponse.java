package com.vegas.analytics.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * GET /api/analytics/trainer/clients — по элементу на клиента тренера.
 * exerciseName — в каком упражнении сигнал (у AWAY и NONE — null);
 * days — "сколько дней": у PLATEAU — без нового максимума, у AWAY — без тренировок, у RECORD — с рекорда;
 * lastWorkoutAt — последняя тренировка (null — ещё не было).
 */
public record ClientSignalResponse(
        UUID clientId,
        ClientSignal signal,
        String exerciseName,
        Integer days,
        Instant lastWorkoutAt
) {
}
