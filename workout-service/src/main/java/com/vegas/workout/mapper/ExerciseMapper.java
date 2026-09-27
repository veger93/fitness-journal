package com.vegas.workout.mapper;

import com.vegas.workout.dto.ExerciseResponse;
import com.vegas.workout.dto.MuscleResponse;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.ExerciseMuscle;
import org.mapstruct.Mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * custom заполнится из Exercise.isCustom() автоматически (boolean-геттер isXxx -> свойство xxx).
 * muscles: Set -> List MapStruct отдаст нашему default-методу, он же задаёт порядок.
 */
@Mapper(componentModel = "spring")
public interface ExerciseMapper {

    ExerciseResponse toResponse(Exercise exercise);

    List<ExerciseResponse> toResponses(List<Exercise> exercises);

    MuscleResponse toMuscleResponse(ExerciseMuscle muscle);

    /** Set не упорядочен -> сортируем сами: сначала PRIMARY, потом SECONDARY, внутри по названию. */
    default List<MuscleResponse> toMuscleResponses(Set<ExerciseMuscle> muscles) {
        return muscles.stream()
                .sorted(Comparator.comparing(ExerciseMuscle::getRole).thenComparing(ExerciseMuscle::getMuscle))
                .map(this::toMuscleResponse)
                .toList();
    }
}
