package com.vegas.workout.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.common.model.BodyPart;
import com.vegas.workout.dto.SetRequest;
import com.vegas.workout.dto.StartWorkoutRequest;
import com.vegas.workout.dto.WorkoutExerciseResponse;
import com.vegas.workout.dto.WorkoutResponse;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutStatus;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.event.OutboxService;
import com.vegas.workout.mapper.WorkoutMapper;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.WorkoutExerciseRepository;
import com.vegas.workout.repository.WorkoutRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private WorkoutRepository workoutRepository;
    @Mock
    private WorkoutExerciseRepository workoutExerciseRepository;
    @Mock
    private WorkoutTemplateRepository templateRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private OutboxService outboxService;

    private WorkoutService service;

    private final UUID me = UUID.randomUUID();
    private final UUID workoutId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkoutService(workoutRepository, workoutExerciseRepository, templateRepository,
                exerciseRepository, new WorkoutMapper(), outboxService, CLOCK);
    }

    // ---------- start ----------

    @Test
    void start_whenAnotherInProgress_throwsConflict() {
        when(workoutRepository.existsByUserIdAndStatus(me, WorkoutStatus.IN_PROGRESS)).thenReturn(true);

        assertThatThrownBy(() -> service.start(me, new StartWorkoutRequest(null, null)))
                .isInstanceOf(BusinessException.class);

        verify(workoutRepository, never()).save(any());
    }

    @Test
    void start_fromTemplate_copiesNameAndExercisesInOrder() {
        Exercise squat = exercise("Приседания со штангой", TrackingType.WEIGHT_REPS);
        Exercise legPress = exercise("Жим ногами", TrackingType.WEIGHT_REPS);
        WorkoutTemplate template = WorkoutTemplate.custom(me, "Ноги", "violet", "steps", List.of(squat, legPress));
        UUID templateId = UUID.randomUUID();
        ReflectionTestUtils.setField(template, "id", templateId);

        when(templateRepository.findWithExercisesById(templateId)).thenReturn(Optional.of(template));
        when(workoutRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutResponse response = service.start(me, new StartWorkoutRequest(templateId, null));

        assertThat(response.name()).isEqualTo("Ноги");
        assertThat(response.templateId()).isEqualTo(templateId);
        assertThat(response.status()).isEqualTo(WorkoutStatus.IN_PROGRESS);
        assertThat(response.startedAt()).isEqualTo(NOW);
        assertThat(response.exercises()).extracting(WorkoutExerciseResponse::exerciseName)
                .containsExactly("Приседания со штангой", "Жим ногами");
    }

    @Test
    void start_fromSomeoneElsesTemplate_isNotFound() {
        WorkoutTemplate foreign = WorkoutTemplate.custom(UUID.randomUUID(), "Чужой", "green", null, List.of());
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findWithExercisesById(templateId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.start(me, new StartWorkoutRequest(templateId, null)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- sets ----------

    @Test
    void addSet_timeExerciseWithoutDuration_throwsBadRequest() {
        Workout workout = workoutWith(exercise("Планка", TrackingType.TIME));
        UUID workoutExerciseId = workout.getExercises().get(0).getId();
        when(workoutRepository.findWithExercisesByIdAndUserId(workoutId, me)).thenReturn(Optional.of(workout));

        SetRequest onlyReps = new SetRequest(null, 10, null, null, null, null);

        assertThatThrownBy(() -> service.addSet(me, workoutId, workoutExerciseId, onlyReps))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addSet_weightReps_isAddedAsNextNumber() {
        Workout workout = workoutWith(exercise("Жим лёжа", TrackingType.WEIGHT_REPS));
        UUID workoutExerciseId = workout.getExercises().get(0).getId();
        when(workoutRepository.findWithExercisesByIdAndUserId(workoutId, me)).thenReturn(Optional.of(workout));

        service.addSet(me, workoutId, workoutExerciseId, new SetRequest(new BigDecimal("80"), 8, null, null, null, false));
        var second = service.addSet(me, workoutId, workoutExerciseId,
                new SetRequest(new BigDecimal("80"), 7, null, null, new BigDecimal("8.5"), null));

        assertThat(second.number()).isEqualTo(2);
        assertThat(second.rpe()).isEqualByComparingTo("8.5");
        assertThat(second.warmup()).isFalse(); // warmup не прислали -> false
    }

    // ---------- complete ----------

    @Test
    void complete_writesEventToOutbox() {
        Workout workout = workoutWith(exercise("Жим лёжа", TrackingType.WEIGHT_REPS));
        ReflectionTestUtils.setField(workout, "id", workoutId);
        UUID workoutExerciseId = workout.getExercises().get(0).getId();
        when(workoutRepository.findWithExercisesByIdAndUserId(workoutId, me)).thenReturn(Optional.of(workout));
        service.addSet(me, workoutId, workoutExerciseId, new SetRequest(new BigDecimal("80"), 8, null, null, null, false));

        WorkoutResponse response = service.complete(me, workoutId);

        assertThat(response.status()).isEqualTo(WorkoutStatus.COMPLETED);
        assertThat(response.tonnageKg()).isEqualByComparingTo("640");

        verify(outboxService).workoutCompleted(workout);
    }

    // ---------- delete ----------

    @Test
    void delete_completedWorkout_notifiesAnalytics() {
        Workout workout = workoutWith(exercise("Жим лёжа", TrackingType.WEIGHT_REPS));
        workout.getExercises().get(0).addSet(
                new com.vegas.workout.entity.SetValues(new BigDecimal("80"), 8, null, null, null, false), NOW);
        workout.complete(NOW);
        when(workoutRepository.findWithExercisesByIdAndUserId(workoutId, me)).thenReturn(Optional.of(workout));

        service.delete(me, workoutId);

        verify(outboxService).workoutDeleted(workout);
        verify(workoutRepository).delete(workout);
    }

    @Test
    void delete_inProgressWorkout_sendsNoEvent() {
        Workout workout = workoutWith(exercise("Жим лёжа", TrackingType.WEIGHT_REPS));
        when(workoutRepository.findWithExercisesByIdAndUserId(workoutId, me)).thenReturn(Optional.of(workout));

        service.delete(me, workoutId);

        verify(outboxService, never()).workoutDeleted(any());
        verify(workoutRepository).delete(workout);
    }

    // ---------- helpers ----------

    /** Идущая тренировка с одним упражнением. id проставляем вручную — БД в тесте нет. */
    private Workout workoutWith(Exercise exercise) {
        Workout workout = Workout.start(me, "Тренировка", null, List.of(exercise), NOW);
        WorkoutExercise workoutExercise = workout.getExercises().get(0);
        ReflectionTestUtils.setField(workoutExercise, "id", UUID.randomUUID());
        return workout;
    }

    private static Exercise exercise(String name, TrackingType trackingType) {
        Exercise exercise = Exercise.custom(UUID.randomUUID(), name, BodyPart.LEGS, Equipment.BARBELL,
                trackingType, null, null);
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(exercise, "ownerId", null); // системное — видно всем
        return exercise;
    }
}
