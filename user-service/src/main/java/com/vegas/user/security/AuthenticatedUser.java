package com.vegas.user.security;

import com.vegas.user.entity.Role;

import java.util.UUID;

/**
 * Кто сделал запрос — данные, достанные из JWT.
 * В контроллере получаем через @AuthenticationPrincipal AuthenticatedUser user.
 * Без похода в БД: всё нужное уже лежит в токене.
 */
public record AuthenticatedUser(UUID id, String email, Role role) {
}
