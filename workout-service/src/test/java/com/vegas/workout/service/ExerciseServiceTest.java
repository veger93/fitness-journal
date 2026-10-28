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
import com.vegas.workout.repository.ExerciseTechniqueRepository;
import com.vegas.workout.repository.WorkoutExerciseRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import com.vegas.workout.dto.DeleteExerciseResponse;
import com.vegas.workout.entity.ExerciseTechnique;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

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
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseTechniqueRepository techniqueRepository;

    @Mock
    private WorkoutExerciseRepository workoutExerciseRepository;

    @Mock
    private WorkoutTemplateRepository templateRepository;

    private final Instant now = Instant.parse("2026-10-28T10:00:00Z");

    private final ExerciseMapper exerciseMapper = Mappers.getMapper(ExerciseMapper.class);

    private ExerciseService service;

    private final UUID me = UUID.randomUUID();
    private final UUID someoneElse = UUID.randomUUID();
    private final UUID exerciseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExerciseService(exerciseRepository, techniqueRepository, workoutExerciseRepository, templateRepository,
                exerciseMapper, Clock.fixed(now, ZoneOffset.UTC));
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
        when(exerciseRepository.existsByOwnerIdAndNameIgnoreCaseAndArchivedAtIsNull(me, "Мой жим")).thenReturn(true);

        assertThatThrownBy(() -> service.create(me, request("Мой жим")))
                .isInstanceOf(BusinessException.class);

        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void create_sameNameAsCatalog_isRejected() {
        when(exerciseRepository.existsByOwnerIdIsNullAndNameIgnoreCase("жим лёжа")).thenReturn(true);

        // иначе прогресс одного и того же упражнения разъехался бы по двум графикам
        assertThatThrownBy(() -> service.create(me, request("жим лёжа")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("уже есть в каталоге");

        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void create_nameOfDeletedExercise_restoresItWithHistory() {
        Exercise deleted = customExercise(me, "Мой жим");
        deleted.archive(now);
        when(exerciseRepository.findFirstByOwnerIdAndNameIgnoreCaseAndArchivedAtIsNotNull(me, "Мой жим"))
                .thenReturn(Optional.of(deleted));

        service.create(me, request("Мой жим"));

        // то же упражнение (с тем же id и историей), а не новое
        assertThat(deleted.isArchived()).isFalse();
        assertThat(deleted.getEquipment()).isEqualTo(Equipment.DUMBBELL);
        verify(exerciseRepository, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_unusedExercise_isDeletedCompletely() {
        Exercise mine = customExercise(me, "Мой жим");
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(mine));

        DeleteExerciseResponse response = service.delete(me, exerciseId);

        assertThat(response.result()).isEqualTo(DeleteExerciseResponse.Result.DELETED);
        verify(exerciseRepository).delete(mine);
    }

    @Test
    void delete_exerciseWithWorkouts_isArchivedNotDeleted() {
        Exercise mine = customExercise(me, "Мой жим");
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(mine));
        when(workoutExerciseRepository.existsByExerciseId(exerciseId)).thenReturn(true);

        DeleteExerciseResponse response = service.delete(me, exerciseId);

        assertThat(response.result()).isEqualTo(DeleteExerciseResponse.Result.ARCHIVED);
        assertThat(mine.getArchivedAt()).isEqualTo(now);
        verify(exerciseRepository, never()).delete(any(Exercise.class));
    }

    @Test
    void delete_exerciseInTemplate_isArchived() {
        Exercise mine = customExercise(me, "Мой жим");
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(mine));
        when(templateRepository.existsByExercisesId(exerciseId)).thenReturn(true);

        assertThat(service.delete(me, exerciseId).result()).isEqualTo(DeleteExerciseResponse.Result.ARCHIVED);
        verify(exerciseRepository, never()).delete(any(Exercise.class));
    }

    @Test
    void delete_systemExercise_isForbidden() {
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(systemExercise()));

        assertThatThrownBy(() -> service.delete(me, exerciseId)).isInstanceOf(ForbiddenException.class);
        verify(exerciseRepository, never()).delete(any(Exercise.class));
    }

    @Test
    void delete_someoneElsesExercise_isNotFound() {
        when(exerciseRepository.findWithMusclesById(exerciseId))
                .thenReturn(Optional.of(customExercise(someoneElse, "Чужой жим")));

        assertThatThrownBy(() -> service.delete(me, exerciseId)).isInstanceOf(NotFoundException.class);
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

    // ---------- getTechnique ----------

    @Test
    void getTechnique_returnsStepsInOrder() {
        Exercise system = systemExercise();
        ReflectionTestUtils.setField(system, "id", exerciseId);
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(system));
        when(techniqueRepository.findById(exerciseId)).thenReturn(Optional.of(technique("Лягте на скамью",
                List.of("Снимите штангу", "Опустите к груди", "Выжмите вверх"), List.of("Таз отрывается"))));

        var response = service.getTechnique(me, exerciseId);

        assertThat(response).isPresent();
        assertThat(response.get().setup()).isEqualTo("Лягте на скамью");
        assertThat(response.get().steps()).containsExactly("Снимите штангу", "Опустите к груди", "Выжмите вверх");
    }

    @Test
    void getTechnique_noTechnique_isEmptyNotError() {
        Exercise mine = customExercise(me, "Мой жим");
        ReflectionTestUtils.setField(mine, "id", exerciseId);
        when(exerciseRepository.findWithMusclesById(exerciseId)).thenReturn(Optional.of(mine));
        when(techniqueRepository.findById(exerciseId)).thenReturn(Optional.empty());

        assertThat(service.getTechnique(me, exerciseId)).isEmpty();
    }

    @Test
    void getTechnique_someoneElsesCustomExercise_isNotFound() {
        when(exerciseRepository.findWithMusclesById(exerciseId))
                .thenReturn(Optional.of(customExercise(someoneElse, "Чужой жим")));

        assertThatThrownBy(() -> service.getTechnique(me, exerciseId))
                .isInstanceOf(NotFoundException.class);
        verify(techniqueRepository, never()).findById(any());
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

    /** Техника тоже создаётся только миграцией: конструктор закрыт, заполняем поля напрямую. */
    private static ExerciseTechnique technique(String setup, List<String> steps, List<String> mistakes) {
        ExerciseTechnique technique = BeanUtils.instantiateClass(ExerciseTechnique.class);
        ReflectionTestUtils.setField(technique, "setup", setup);
        ReflectionTestUtils.setField(technique, "steps", steps);
        ReflectionTestUtils.setField(technique, "mistakes", mistakes);
        return technique;
    }

    /** Системные упражнения создаёт только миграция, поэтому в тесте "обнуляем" владельца. */
    private static Exercise systemExercise() {
        Exercise exercise = customExercise(UUID.randomUUID(), "Жим лёжа");
        ReflectionTestUtils.setField(exercise, "ownerId", null);
        return exercise;
    }
}
