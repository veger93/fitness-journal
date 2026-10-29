package com.vegas.workout.security;

import com.vegas.traineraccess.TrainerAccessFilter;
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

    /**
     * Кто на самом деле действует: тренер, если он работает за клиента (заголовок X-Client-Id), иначе null.
     * При этом id(jwt) — уже id клиента: подмену делает TrainerAccessFilter.
     */
    public static UUID actorId(Jwt jwt) {
        return TrainerAccessFilter.actorId(jwt);
    }
}
