package com.vegas.user.dto;

import java.time.Instant;
import java.util.UUID;

/** Мой тренер — для карточки в профиле клиента */
public record MyTrainerResponse(UUID trainerId, String trainerName, Instant since) {
}
