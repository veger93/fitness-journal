package com.vegas.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Программа тренировок изменилась: собрали новую по анкете или отказались от текущей (docs/program.md).
 * workout-service -> notification-service: напомнить «пора пересмотреть программу» в день reviewAt.
 *
 * Как WeeklyPlanUpdatedEvent, несёт состояние ЦЕЛИКОМ: programId и reviewAt == null — активной программы нет.
 * changedAt — чтобы получатель отбросил событие, пришедшее позже более нового.
 */
public record ProgramChangedEvent(
        UUID userId,
        UUID programId,
        LocalDate reviewAt,
        Instant changedAt
) {
    public static ProgramChangedEvent started(UUID userId, UUID programId, LocalDate reviewAt, Instant now) {
        return new ProgramChangedEvent(userId, programId, reviewAt, now);
    }

    public static ProgramChangedEvent none(UUID userId, Instant now) {
        return new ProgramChangedEvent(userId, null, null, now);
    }

    public boolean hasProgram() {
        return programId != null;
    }
}
