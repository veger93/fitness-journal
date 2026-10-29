package com.vegas.workout.mapper;

import com.vegas.workout.dto.DeloadResponse;
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

    /** Для завершённых тренировок: подсказки (прошлые подходы, разгрузка) не нужны. */
    public WorkoutResponse toResponse(Workout workout) {
        return toResponse(workout, Map.of(), Map.of());
    }

    /**
     * @param previousSets exerciseId -> подходы этого упражнения в прошлой тренировке
     * @param deloads      exerciseId -> активная разгрузка
     */
    public WorkoutResponse toResponse(Workout workout, Map<UUID, List<WorkoutSet>> previousSets,
                                      Map<UUID, DeloadResponse> deloads) {
        List<WorkoutExerciseResponse> exercises = workout.getExercises().stream()
                .map(workoutExercise -> {
                    UUID exerciseId = workoutExercise.getExercise().getId();
                    return toExerciseResponse(workoutExercise, previousSets.getOrDefault(exerciseId, List.of()),
                            deloads.get(exerciseId));
                })
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
                workout.getRecordedBy() != null,
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

    private WorkoutExerciseResponse toExerciseResponse(WorkoutExercise workoutExercise, List<WorkoutSet> previous,
                                                       DeloadResponse deload) {
        Exercise exercise = workoutExercise.getExercise();
        return new WorkoutExerciseResponse(
                workoutExercise.getId(),
                exercise.getId(),
                exercise.getName(),
                exercise.getTrackingType(),
                exercise.getEquipment(),
                toSetResponses(workoutExercise.getSets()),
                toSetResponses(previous),
                deload
        );
    }

    private List<SetResponse> toSetResponses(List<WorkoutSet> sets) {
        return sets.stream().map(this::toSetResponse).toList();
    }
}
