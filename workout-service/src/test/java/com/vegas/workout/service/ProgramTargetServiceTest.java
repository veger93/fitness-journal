package com.vegas.workout.service;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.dto.ExerciseTargetResponse;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.Program;
import com.vegas.workout.entity.ProgramAnswers;
import com.vegas.workout.entity.ProgramGoal;
import com.vegas.workout.entity.ProgramLocation;
import com.vegas.workout.entity.ExperienceLevel;
import com.vegas.workout.entity.ProgramSlot;
import com.vegas.workout.entity.SetScheme;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.repository.ProgramRepository;
import com.vegas.workout.repository.WorkoutExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramTargetServiceTest {

    @Mock private ProgramRepository programRepository;
    @Mock private WorkoutExerciseRepository workoutExerciseRepository;
    @InjectMocks private ProgramTargetService service;

    private final UUID me = UUID.randomUUID();

    @Test
    void freeWorkout_noTargets() {
        Workout free = Workout.create(me, "Своя", null, List.of(exercise("Жим лёжа")));

        assertThat(service.targets(free)).isEmpty();
        verifyNoInteractions(programRepository);
    }

    @Test
    void programDay_targetFromSlot_extraExerciseWithout() {
        Exercise bench = exercise("Жим лёжа");
        Exercise extra = exercise("Пресс");
        UUID templateId = UUID.randomUUID();
        Workout workout = Workout.create(me, "День A", templateId, List.of(bench, extra));
        WorkoutTemplate day = WorkoutTemplate.custom(me, "День A", "orange", null, List.of(bench));
        Program program = Program.start(me, new ProgramAnswers(ProgramGoal.MUSCLE, 3, 60, ProgramLocation.GYM,
                ExperienceLevel.BEGINNER, null, null), List.of(), Instant.now(), LocalDate.now());
        ProgramSlot slot = program.addSlot(day, 0, bench, SetScheme.of(3, 8, 12, 120));
        when(programRepository.findActiveSlots(me, templateId)).thenReturn(List.of(slot));
        when(workoutExerciseRepository.findRecent(eq(me), eq(bench.getId()), any(), any())).thenReturn(List.of());

        Map<UUID, ExerciseTargetResponse> targets = service.targets(workout);

        assertThat(targets).containsOnlyKeys(bench.getId());
        ExerciseTargetResponse target = targets.get(bench.getId());
        assertThat(target.sets()).isEqualTo(3);
        assertThat(target.repsMin()).isEqualTo(8);
        assertThat(target.repsMax()).isEqualTo(12);
        assertThat(target.suggestion()).isEqualTo("NEW");
    }

    private static Exercise exercise(String name) {
        Exercise e = Exercise.custom(UUID.randomUUID(), name, BodyPart.CHEST, Equipment.BARBELL,
                TrackingType.WEIGHT_REPS, null, null);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(e, "movementPattern", "HORIZONTAL_PUSH");
        return e;
    }
}
