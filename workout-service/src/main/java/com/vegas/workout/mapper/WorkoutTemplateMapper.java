package com.vegas.workout.mapper;

import com.vegas.workout.dto.TemplateExerciseResponse;
import com.vegas.workout.dto.WorkoutTemplateResponse;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.WorkoutTemplate;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkoutTemplateMapper {

    WorkoutTemplateResponse toResponse(WorkoutTemplate template);

    List<WorkoutTemplateResponse> toResponses(List<WorkoutTemplate> templates);

    /** List<Exercise> -> List<TemplateExerciseResponse> MapStruct соберёт сам через этот метод. */
    TemplateExerciseResponse toExerciseResponse(Exercise exercise);
}
