package com.vegas.user.dto;

import java.time.Instant;

/** Что клиент видит, открыв ссылку: чей дневник и от кого. Больше ничего — до входа данные не показываем */
public record ClaimPreviewResponse(String clientName, String trainerName, Instant expiresAt) {
}
