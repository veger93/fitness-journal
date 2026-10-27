package com.vegas.workout.dto;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.TrackingType;

import java.util.List;
import java.util.UUID;

/**
 * custom=true — своё упражнение пользователя (можно редактировать), false — системное.
 * muscles — какие мышцы работают (карточка "как выполнять"); у своих упражнений пустой список.
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
        List<MuscleResponse> muscles,
        String imageKey     // картинка "начало → конец" (иллюстрации Everkinetic); null — картинки нет
) {
}
