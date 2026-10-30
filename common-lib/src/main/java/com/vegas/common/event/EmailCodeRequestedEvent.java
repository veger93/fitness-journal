package com.vegas.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * "Отправьте человеку код из 6 цифр" — user-service просит notification-service (только он умеет слать письма).
 * purpose: VERIFY_EMAIL — подтвердить email, RESET_PASSWORD — сменить забытый пароль.
 * Код идёт во внутренней сети между нашими сервисами; в базе user-service хранится только его хеш.
 *
 * trainerName и keepTrainer — только когда тренер передал клиенту аккаунт (docs/trainer.md, шаг 5.3):
 * письмо с кодом становится приветственным — "тренер Сергей передал вам дневник". Иначе оба null.
 */
public record EmailCodeRequestedEvent(
        UUID userId,
        String email,
        String displayName,
        String code,
        String purpose,
        Instant expiresAt,
        String trainerName,
        Boolean keepTrainer
) {
}
