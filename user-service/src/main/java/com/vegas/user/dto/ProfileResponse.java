package com.vegas.user.dto;

import com.vegas.common.model.Gender;
import com.vegas.user.entity.ExperienceLevel;

import java.math.BigDecimal;

/**
 * Профиль для фронта. onboardingCompleted=false -> после логина показываем экран онбординга.
 * currentWeightKg берётся из последней записи body_weight_log (может быть null до онбординга).
 */
public record ProfileResponse(
        Gender gender,
        Short birthYear,
        Short heightCm,
        ExperienceLevel experienceLevel,
        boolean onboardingCompleted,
        BigDecimal currentWeightKg
) {
}
