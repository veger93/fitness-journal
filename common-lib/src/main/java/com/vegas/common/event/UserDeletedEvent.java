package com.vegas.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * "Пользователь удалён" — каждый сервис удаляет у себя все его данные (тренировки, аналитику, уведомления).
 * Сейчас так тренер удаляет клиента без аккаунта (docs/trainer.md); позже — и "удалить мой аккаунт".
 */
public record UserDeletedEvent(
        UUID userId,
        Instant deletedAt
) {
}
