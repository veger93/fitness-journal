package com.vegas.workout.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.ForbiddenException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.common.model.BodyPart;
import com.vegas.common.model.MuscleGroup;
import com.vegas.workout.dto.ExerciseRequest;
import com.vegas.workout.dto.ExerciseResponse;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.ExerciseMuscle;
import com.vegas.workout.entity.MuscleRole;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.mapper.ExerciseMapper;
import com.vegas.workout.repository.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    private final ExerciseMapper exerciseMapper = Mappers.getMapper(ExerciseMapper.class);

    private ExerciseService service;

    private final UUID me = UUID.randomUUID();
    private final UUID someoneElse = UUID.randomUUID();
    private final UUID exerciseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExerciseService(exerciseRepository, exerciseMapper);
    }

    // ---------- create ----------

    @Test
    void create_trimsNameAndSetsOwner() {
        when(exerciseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExerciseResponse response = service.create(me, request("  Жим гантелей на наклонной  "));

        ArgumentCaptor<Exercise> saved = ArgumentCaptor.forClass(Exercise.class);
        verify(exerciseRepository).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(me);
        assertThat(saved.getValue().getName()).isEqualTo("Жим гантелей на наклонной");
        assertThat(response.custom()).isTrue();
        assertThat(response.muscles()).isEmpty();
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(exerciseRepository.existsByOwnerIdAndNameIgnoreCase(me, "Мой жим")).thenReturn(true);

        assertThatThrownBy(() -> service.create(me, request("Мой жим")))
                .isInstanceOf(BusinessException.class);

        verify(exerciseRepository, never()).save(any());
    }

    // ---------- getById ----------

    @Test
    void getById_systemExercise_visibleToEveryone() {
        Exercise system = systemExercise();
        system.getMuscles().add(new ExerciseMuscle(MuscleGroup.TRICEPS, MuscleRole.SECONDARY));
        system.getMuscles().add(new ExerciseMuscle(MuscleGroup.CHEST, MuscleRole.PRIMARY));
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(system));

        ExerciseResponse response = service.getById(me, exerciseId);

        assertThat(response.custom()).isFalse();
        // PRIMARY всегда первым, независимо от порядка в Set
        assertThat(response.muscles().get(0).muscle()).isEqualTo(MuscleGroup.CHEST);
        assertThat(response.muscles().get(0).role()).isEqualTo(MuscleRole.PRIMARY);
    }

    @Test
    void getById_someoneElsesCustomExercise_isNotFound() {
        when(exerciseRepository.findWithMusclesById(exerciseId))
                .thenReturn(Optional.of(customExercise(someoneElse, "Чужой жим")));

        assertThatThrownBy(() -> service.getById(me, exerciseId))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- update ----------

    @Test
    void update_ownExercise_changesFields() {
        Exercise mine = customExercise(me, "Старое название");
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(mine));

        ExerciseResponse response = service.update(me, exerciseId, request("Новое название"));

        assertThat(mine.getName()).isEqualTo("Новое название");
        assertThat(response.name()).isEqualTo("Новое название");
    }

    @Test
    void update_systemExercise_isForbidden() {
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(systemExercise()));

        assertThatThrownBy(() -> service.update(me, exerciseId, request("Мой жим лёжа")))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------- helpers ----------

    private static ExerciseRequest request(String name) {
        return new ExerciseRequest(name, BodyPart.CHEST, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, "dumbbell", null);
    }

    private static Exercise customExercise(UUID ownerId, String name) {
        return Exercise.custom(ownerId, name, BodyPart.CHEST, Equipment.BARBELL, TrackingType.WEIGHT_REPS, null, null);
    }

    /** Системные упражнения создаёт только миграция, поэтому в тесте "обнуляем" владельца. */
    private static Exercise systemExercise() {
        Exercise exercise = customExercise(UUID.randomUUID(), "Жим лёжа");
        ReflectionTestUtils.setField(exercise, "ownerId", null);
        return exercise;
    }
}
