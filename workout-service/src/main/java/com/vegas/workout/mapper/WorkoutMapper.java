package com.vegas.workout.mapper;

import com.vegas.workout.dto.SetResponse;
import com.vegas.workout.dto.WorkoutExerciseResponse;
import com.vegas.workout.dto.WorkoutResponse;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Маппер, написанный руками. MapStruct удобен для "поле в поле", а здесь много вычисляемого:
 * длительность, тоннаж, номер подхода, подходы прошлой тренировки. Обычный Java-код читается проще.
 */
@Component
public class WorkoutMapper {

    /**
     * @param previousSets exerciseId -> подходы этого упражнения в прошлой тренировке
     */
    public WorkoutResponse toResponse(Workout workout, Map<UUID, List<WorkoutSet>> previousSets) {
        List<WorkoutExerciseResponse> exercises = workout.getExercises().stream()
                .map(workoutExercise -> toExerciseResponse(
                        workoutExercise,
                        previousSets.getOrDefault(workoutExercise.getExercise().getId(), List.of())))
                .toList();

        return new WorkoutResponse(
                workout.getId(),
                workout.getName(),
                workout.getTemplateId(),
                workout.getStatus(),
                workout.getStartedAt(),
                workout.getCompletedAt(),
                workout.getDurationSec(),
                workout.getWorkingSetsCount(),
                workout.getTonnageKg(),
                exercises
        );
    }

    public SetResponse toSetResponse(WorkoutSet set) {
        return new SetResponse(
                set.getId(),
                set.getPosition() + 1,  // в БД нумерация с 0, на экране — с 1
                set.getWeightKg(),
                set.getReps(),
                set.getDurationSec(),
                set.getDistanceM(),
                set.getRpe(),
                set.isWarmup()
        );
    }

    private WorkoutExerciseResponse toExerciseResponse(WorkoutExercise workoutExercise, List<WorkoutSet> previous) {
        Exercise exercise = workoutExercise.getExercise();
        return new WorkoutExerciseResponse(
                workoutExercise.getId(),
                exercise.getId(),
                exercise.getName(),
                exercise.getTrackingType(),
                toSetResponses(workoutExercise.getSets()),
                toSetResponses(previous)
        );
    }

    private List<SetResponse> toSetResponses(List<WorkoutSet> sets) {
        return sets.stream().map(this::toSetResponse).toList();
    }
}
