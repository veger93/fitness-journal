package com.vegas.workout.event;

import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutCompletedEvent.ExercisePerformed;
import com.vegas.common.event.WorkoutCompletedEvent.SetPerformed;
import com.vegas.common.event.WorkoutDeletedEvent;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutSet;

/** Сборка событий из сущностей: внутренняя модель -> публичный контракт из common-lib. */
public final class WorkoutEvents {

    private WorkoutEvents() {
    }

    public static WorkoutCompletedEvent completed(Workout workout) {
        return new WorkoutCompletedEvent(
                workout.getId(),
                workout.getUserId(),
                workout.getCompletedAt(),
                workout.getExercises().stream().map(WorkoutEvents::toExercisePerformed).toList()
        );
    }

    public static WorkoutDeletedEvent deleted(Workout workout) {
        return new WorkoutDeletedEvent(workout.getId(), workout.getUserId());
    }

    private static ExercisePerformed toExercisePerformed(WorkoutExercise workoutExercise) {
        Exercise exercise = workoutExercise.getExercise();
        return new ExercisePerformed(
                exercise.getId(),
                exercise.getName(),
                exercise.getBodyPart(),
                exercise.getTrackingType().name(),
                workoutExercise.getSets().stream().map(WorkoutEvents::toSetPerformed).toList()
        );
    }

    private static SetPerformed toSetPerformed(WorkoutSet set) {
        return new SetPerformed(set.getWeightKg(), set.getReps(), set.getDurationSec(),
                set.getDistanceM(), set.getRpe(), set.isWarmup());
    }
}
