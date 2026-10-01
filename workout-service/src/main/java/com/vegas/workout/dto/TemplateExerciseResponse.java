package com.vegas.workout.dto;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.TrackingType;

import java.util.UUID;

/**
 * Упражнение внутри комплекса — облегчённая версия без мышц:
 * для списка комплексов мышцы не нужны, а их загрузка дала бы лишние запросы.
 */
public record TemplateExerciseResponse(
        UUID id,
        String name,
        BodyPart bodyPart,
        Equipment equipment,
        TrackingType trackingType,
        String icon
) {
}
