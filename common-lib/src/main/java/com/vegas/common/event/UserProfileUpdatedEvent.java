package com.vegas.common.event;

import com.vegas.common.model.Gender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * "Профиль пользователя изменился" — снимок физ. данных целиком (а не только изменённое поле).
 * Публикует user-service при сохранении профиля и записи веса, слушает analytics-service
 * (уровень подготовки для эталонной кривой; пол и вес — для будущих силовых стандартов).
 *
 * updatedAt — момент изменения: получатель по нему отбрасывает устаревшие события,
 * если они вдруг придут не по порядку.
 * experienceLevel — строка (BEGINNER / INTERMEDIATE / ADVANCED): enum живёт в user-service.
 */
public record UserProfileUpdatedEvent(
        UUID userId,
        Gender gender,
        Integer birthYear,
        Integer heightCm,
        String experienceLevel,
        BigDecimal bodyWeightKg,
        Instant updatedAt
) {
}
