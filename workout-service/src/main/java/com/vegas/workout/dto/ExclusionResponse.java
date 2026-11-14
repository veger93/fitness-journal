package com.vegas.workout.dto;

import java.util.UUID;

/** «Не предлагать: жим штанги лёжа» в профиле — с кнопкой «Вернуть» */
public record ExclusionResponse(UUID exerciseId, String exerciseName) {
}
