package com.vegas.workout.entity;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тест доменной модели без Spring и без моков: правила живут в самих сущностях,
 * поэтому их можно проверить обычными вызовами методов.
 */
class WorkoutTest {

    private static final Instant START = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant END = Instant.parse("2026-10-01T11:05:00Z");

    private final Exercise bench = exercise("Жим лёжа");
    private final Exercise squat = exercise("Приседания со штангой");

    @Test
    void tonnageAndWorkingSets_ignoreWarmups() {
        Workout workout = Workout.start(UUID.randomUUID(), "Торс", null, List.of(bench), START);
        WorkoutExercise benchInWorkout = workout.getExercises().get(0);

        benchInWorkout.addSet(weightReps("40", 10, true), START);   // разминка — не считается
        benchInWorkout.addSet(weightReps("80", 8, false), START);   // 640
        benchInWorkout.addSet(weightReps("80", 6, false), START);   // 480

        assertThat(workout.getWorkingSetsCount()).isEqualTo(2);
        assertThat(workout.getTonnageKg()).isEqualByComparingTo("1120");
    }

    @Test
    void complete_setsStatusAndDuration() {
        Workout workout = Workout.start(UUID.randomUUID(), "Торс", null, List.of(bench), START);
        workout.getExercises().get(0).addSet(weightReps("80", 8, false), START);

        workout.complete(END);

        assertThat(workout.getStatus()).isEqualTo(WorkoutStatus.COMPLETED);
        assertThat(workout.getDurationSec()).isEqualTo(65 * 60);
    }

    @Test
    void complete_withoutSets_isRejected() {
        Workout workout = Workout.start(UUID.randomUUID(), "Торс", null, List.of(bench), START);

        assertThatThrownBy(() -> workout.complete(END)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void completedWorkout_cannotBeChanged() {
        Workout workout = Workout.start(UUID.randomUUID(), "Торс", null, List.of(bench), START);
        WorkoutExercise benchInWorkout = workout.getExercises().get(0);
        benchInWorkout.addSet(weightReps("80", 8, false), START);
        workout.complete(END);

        assertThatThrownBy(() -> benchInWorkout.addSet(weightReps("80", 8, false), END))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> workout.addExercise(squat))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void sameExerciseTwice_isRejected() {
        Workout workout = Workout.start(UUID.randomUUID(), "Торс", null, List.of(bench), START);

        assertThatThrownBy(() -> workout.addExercise(bench)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void removeExercise_renumbersTheRest() {
        Workout workout = Workout.start(UUID.randomUUID(), "Фулбади", null, List.of(bench, squat), START);
        WorkoutExercise first = workout.getExercises().get(0);
        ReflectionTestUtils.setField(first, "id", UUID.randomUUID());

        workout.removeExercise(first.getId());

        assertThat(workout.getExercises()).hasSize(1);
        assertThat(workout.getExercises().get(0).getExercise()).isSameAs(squat);
        assertThat(workout.getExercises().get(0).getPosition()).isZero(); // без "дыры" в нумерации
    }

    private static SetValues weightReps(String weight, int reps, boolean warmup) {
        return new SetValues(new BigDecimal(weight), reps, null, null, null, warmup);
    }

    private static Exercise exercise(String name) {
        Exercise exercise = Exercise.custom(UUID.randomUUID(), name, BodyPart.CHEST, Equipment.BARBELL,
                TrackingType.WEIGHT_REPS, null, null);
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        return exercise;
    }
}
