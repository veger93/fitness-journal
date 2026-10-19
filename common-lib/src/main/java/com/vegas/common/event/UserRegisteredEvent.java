package com.vegas.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * "Зарегистрирован новый пользователь". Слушает notification-service: ему нужен email,
 * чтобы было куда отправлять письма. Пароль и прочее сюда, разумеется, не попадает.
 */
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String displayName,
        Instant registeredAt
) {
}
