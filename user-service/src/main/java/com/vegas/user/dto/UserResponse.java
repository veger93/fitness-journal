package com.vegas.user.dto;

import com.vegas.user.entity.AuthProvider;
import com.vegas.user.entity.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Пользователь, каким его видит фронт.
 * Сущность User наружу НЕ отдаём: там passwordHash, и любое изменение таблицы сломало бы API.
 */
public record UserResponse(
        UUID id,
        String email,
        /** email подтверждён кодом из письма (иначе — плашка "Подтвердите email") */
        boolean emailVerified,
        String displayName,
        AuthProvider authProvider,
        Role role,
        /** режим тренера включён */
        boolean trainer,
        Instant createdAt,
        /** демо "посмотреть изнутри" (docs/demo.md): полоса "ДЕМО", без смены email и удаления аккаунта */
        boolean demo,
        /** когда демо удалится; null у настоящих пользователей */
        Instant demoExpiresAt
) {
}
