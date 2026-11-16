package com.vegas.workout.program;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.SetValues;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutSet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** П10, двойная прогрессия: прибавка — только после двух тренировок подряд на верхней границе */
class ProgressionTest {

    private static final Exercise BENCH = Exercise.custom(UUID.randomUUID(), "Жим лёжа", BodyPart.CHEST,
            Equipment.BARBELL, TrackingType.WEIGHT_REPS, null, null);

    @Test
    void twiceAtTop_increaseByStep() {
        var hint = Progression.suggest(List.of(sets("60x12", "60x12", "60x12"), sets("60x12", "60x12", "60x12")),
                12, Equipment.BARBELL);

        assertThat(hint.kind()).isEqualTo(Progression.Kind.INCREASE);
        assertThat(hint.suggestedWeightKg()).isEqualByComparingTo("62.5");
        assertThat(hint.lastReps()).containsExactly(12, 12, 12);
    }

    @Test
    void onlyOnceAtTop_keep() {
        var hint = Progression.suggest(List.of(sets("60x12", "60x12", "60x12"), sets("60x12", "60x10", "60x9")),
                12, Equipment.BARBELL);

        assertThat(hint.kind()).isEqualTo(Progression.Kind.KEEP);
        assertThat(hint.suggestedWeightKg()).isEqualByComparingTo("60");
    }

    /** Случай Сергея: 4 подхода по 60 и одна проба на 65 — рабочий вес 60, проба прибавку не включает */
    @Test
    void oneHeavierProbe_workingWeightIsTheCommonOne() {
        var hint = Progression.suggest(List.of(sets("60x10", "60x10", "60x10", "65x6", "60x9")), 12, Equipment.BARBELL);

        assertThat(hint.kind()).isEqualTo(Progression.Kind.KEEP);
        assertThat(hint.baseWeightKg()).isEqualByComparingTo("60");
        assertThat(hint.lastReps()).containsExactly(10, 10, 10, 9);
    }

    @Test
    void workingWeightChangedBetweenWorkouts_keep() {
        var hint = Progression.suggest(List.of(sets("62.5x12", "62.5x12"), sets("60x12", "60x12")), 12, Equipment.BARBELL);

        assertThat(hint.kind()).isEqualTo(Progression.Kind.KEEP);
    }

    @Test
    void warmupIgnored_machineStepOne() {
        var hint = Progression.suggest(List.of(sets("w20x10", "46x15", "46x15"), sets("w20x10", "46x15", "46x15")),
                15, Equipment.MACHINE);

        assertThat(hint.kind()).isEqualTo(Progression.Kind.INCREASE);
        assertThat(hint.suggestedWeightKg()).isEqualByComparingTo("47");
    }

    @Test
    void noHistoryOrNoWeight_new() {
        assertThat(Progression.suggest(List.of(), 12, Equipment.BARBELL).kind()).isEqualTo(Progression.Kind.NEW);
        assertThat(Progression.suggest(List.of(sets("x12", "x10")), 12, Equipment.BODYWEIGHT).kind())
                .isEqualTo(Progression.Kind.NEW);
    }

    /** «60x12» — вес × повторы, «w» — разминка, «x12» — без веса */
    private static List<WorkoutSet> sets(String... specs) {
        Workout workout = Workout.create(UUID.randomUUID(), "Т", null, List.of(BENCH));
        WorkoutExercise exercise = workout.getExercises().get(0);
        for (String spec : specs) {
            boolean warmup = spec.startsWith("w");
            String[] parts = spec.replace("w", "").split("x");
            BigDecimal weight = parts[0].isEmpty() ? null : new BigDecimal(parts[0]);
            exercise.addSet(new SetValues(weight, Integer.valueOf(parts[1]), null, null, null, warmup), Instant.now());
        }
        return exercise.getSets();
    }
}
