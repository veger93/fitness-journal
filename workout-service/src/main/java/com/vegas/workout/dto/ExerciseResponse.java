package com.vegas.workout.dto;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.TrackingType;

import java.util.List;
import java.util.UUID;

/**
 * custom=true — своё упражнение пользователя (можно редактировать), false — системное.
 * muscles — для подсветки на карте тела; у своих упражнений пустой список,
 * тогда фронт подсвечивает всю bodyPart.
 */
public record ExerciseResponse(
        UUID id,
        String name,
        BodyPart bodyPart,
        Equipment equipment,
        TrackingType trackingType,
        String icon,
        String note,
        boolean custom,
        List<MuscleResponse> muscles
) {
}
