package com.vegas.workout.dto;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.TrackingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Создание (POST) и редактирование (PUT) своего упражнения — поля экрана "Своё упражнение".
 */
public record ExerciseRequest(

        @NotBlank(message = "название обязательно")
        @Size(max = 100, message = "название не длиннее 100 символов")
        String name,

        @NotNull(message = "группа мышц обязательна")
        BodyPart bodyPart,

        @NotNull(message = "тип нагрузки обязателен")
        Equipment equipment,

        @NotNull(message = "что записываем — обязательно")
        TrackingType trackingType,

        // ключ иконки из набора на фронте: латиница, цифры, дефис
        @Pattern(regexp = "^[a-z0-9-]{1,30}$", message = "некорректный ключ иконки")
        String icon,

        @Size(max = 1000, message = "заметка не длиннее 1000 символов")
        String note
) {
}
