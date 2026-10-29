package com.vegas.user.dto;

import com.vegas.user.entity.ExperienceLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Клиент в списке тренера.
 * managed = true — ведомый (входа нет, всё ведёт тренер); false — у клиента свой вход (аккаунт передан).
 */
public record ClientResponse(
        UUID id,
        String displayName,
        String phone,
        boolean managed,
        Short heightCm,
        ExperienceLevel experienceLevel,
        BigDecimal currentWeightKg,
        Instant linkedAt
) {
}
