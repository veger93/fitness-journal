package com.vegas.user.dto;

import java.time.Instant;

/** Ссылка передачи аккаунта: фронт собирает из токена адрес https://vegas-fit.ru/claim/<token> и QR-код */
public record ClaimLinkResponse(String token, Instant expiresAt) {
}
