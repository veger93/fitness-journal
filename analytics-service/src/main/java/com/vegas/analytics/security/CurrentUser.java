package com.vegas.analytics.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** id пользователя из поля "sub" JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
