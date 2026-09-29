package com.vegas.workout.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * id пользователя из токена. В user-service мы кладём его в стандартное поле "sub" (subject).
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
