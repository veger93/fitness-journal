package com.vegas.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * "Заполни дневник демо-пользователя" (docs/demo.md): workout-service генерирует тренировки по сценарию,
 * даты — относительно requestedAt. Событие на каждого человека демо: у демо-тренера это он сам и его клиенты.
 *
 * scenario — ATHLETE, COACH, CLIENT_IN_PROGRESS, CLIENT_OVERLOAD, CLIENT_PLATEAU, CLIENT_AWAY, CLIENT_RECORD;
 * recordedBy — кто "записывал" тренировки: у клиентов демо-тренера — тренер, иначе null.
 */
public record DemoDataRequestedEvent(
        UUID userId,
        String scenario,
        UUID recordedBy,
        Instant requestedAt
) {
}
