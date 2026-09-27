package com.vegas.workout.dto;

import com.vegas.common.model.MuscleGroup;
import com.vegas.workout.entity.MuscleRole;

public record MuscleResponse(
        MuscleGroup muscle,
        MuscleRole role
) {
}
