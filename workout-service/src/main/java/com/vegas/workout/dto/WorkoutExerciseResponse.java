package com.vegas.workout.dto;

import com.vegas.workout.entity.TrackingType;

import java.util.List;
import java.util.UUID;

/**
 * id — это id упражнения ВНУТРИ тренировки (для URL .../exercises/{id}/sets),
 * exerciseId — id упражнения из каталога.
 * previousSets — подходы из прошлой тренировки с этим упражнением
 * ("Прошлый раз на этом подходе: 80 кг × 8"). Только у идущей тренировки.
 */
public record WorkoutExerciseResponse(
        UUID id,
        UUID exerciseId,
        String exerciseName,
        TrackingType trackingType,
        List<SetResponse> sets,
        List<SetResponse> previousSets
) {
}
