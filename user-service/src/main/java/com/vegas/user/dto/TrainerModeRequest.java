package com.vegas.user.dto;

import jakarta.validation.constraints.NotNull;

/** PUT /api/users/me/trainer-mode — переключатель "Я тренер" в профиле */
public record TrainerModeRequest(
        @NotNull(message = "enabled обязателен")
        Boolean enabled
) {
}
