package com.vegas.common.event;

import java.time.Instant;
import java.util.UUID;

/** Плато по упражнению. lastProgressAt — начало плато (по нему получатель не дублирует уведомление). */
public record PlateauDetectedEvent(
        UUID userId,
        UUID exerciseId,
        String exerciseName,
        Instant lastProgressAt,
        long days
) {
}
