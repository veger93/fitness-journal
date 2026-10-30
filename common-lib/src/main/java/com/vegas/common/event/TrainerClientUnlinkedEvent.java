package com.vegas.common.event;

import java.time.Instant;
import java.util.UUID;

/** "Клиент ушёл от тренера": копии связей в workout/analytics её удаляют — доступ тренера пропадает. Ключ — clientId */
public record TrainerClientUnlinkedEvent(
        UUID trainerId,
        UUID clientId,
        Instant unlinkedAt
) {
}
