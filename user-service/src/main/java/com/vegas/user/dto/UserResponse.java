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
        String displayName,
        AuthProvider authProvider,
        Role role,
        Instant createdAt
) {
}
