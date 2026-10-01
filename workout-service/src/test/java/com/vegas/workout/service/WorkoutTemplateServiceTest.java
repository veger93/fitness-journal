package com.vegas.workout.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.ForbiddenException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.common.model.BodyPart;
import com.vegas.workout.dto.TemplateExerciseResponse;
import com.vegas.workout.dto.WorkoutTemplateRequest;
import com.vegas.workout.dto.WorkoutTemplateResponse;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.mapper.WorkoutTemplateMapper;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class WorkoutTemplateServiceTest {

    @Mock
    private WorkoutTemplateRepository templateRepository;
    @Mock
    private ExerciseRepository exerciseRepository;

    private final WorkoutTemplateMapper templateMapper = Mappers.getMapper(WorkoutTemplateMapper.class);

    private WorkoutTemplateService service;

    private final UUID me = UUID.randomUUID();
    private final UUID someoneElse = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();

    private Exercise squat;
    private Exercise bench;
    private Exercise foreignCustom;

    @BeforeEach
    void setUp() {
        service = new WorkoutTemplateService(templateRepository, exerciseRepository, templateMapper);
        squat = systemExercise("Приседания со штангой");
        bench = systemExercise("Жим лёжа");
        foreignCustom = exercise(someoneElse, "Чужое упражнение");
    }

    // ---------- create ----------

    @Test
    void create_keepsExerciseOrderFromRequest() {
        // репозиторий отдаёт в "своём" порядке: bench, squat
        when(exerciseRepository.findAllById(List.of(squat.getId(), bench.getId()))).thenReturn(List.of(bench, squat));
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutTemplateResponse response = service.create(me, request("Мои ноги", squat.getId(), bench.getId()));

        // а в комплексе — порядок из запроса: squat, bench
        assertThat(response.exercises()).extracting(TemplateExerciseResponse::name).containsExactly("Приседания со штангой", "Жим лёжа");
        assertThat(response.custom()).isTrue();

        ArgumentCaptor<WorkoutTemplate> saved = ArgumentCaptor.forClass(WorkoutTemplate.class);
        verify(templateRepository).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(me);
    }

    @Test
    void create_duplicateExercise_throwsBadRequest() {
        assertThatThrownBy(() -> service.create(me, request("Дубли", squat.getId(), squat.getId())))
                .isInstanceOf(BadRequestException.class);

        verify(templateRepository, never()).save(any());
    }

    @Test
    void create_withSomeoneElsesExercise_throwsBadRequest() {
        when(exerciseRepository.findAllById(List.of(foreignCustom.getId()))).thenReturn(List.of(foreignCustom));

        assertThatThrownBy(() -> service.create(me, request("Чужое", foreignCustom.getId())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_nameTaken_throwsConflict() {
        when(templateRepository.existsByOwnerIdAndNameIgnoreCase(me, "Ноги")).thenReturn(true);

        assertThatThrownBy(() -> service.create(me, request(" Ноги ", squat.getId())))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- update / delete ----------

    @Test
    void update_readyTemplate_isForbidden() {
        when(templateRepository.findWithExercisesById(templateId)).thenReturn(Optional.of(readyTemplate()));

        assertThatThrownBy(() -> service.update(me, templateId, request("Ноги", squat.getId())))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_someoneElsesTemplate_isNotFound() {
        WorkoutTemplate foreign = WorkoutTemplate.custom(someoneElse, "Чужой", "green", null, List.of(squat));
        when(templateRepository.findWithExercisesById(templateId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.delete(me, templateId))
                .isInstanceOf(NotFoundException.class);

        verify(templateRepository, never()).delete(any());
    }

    // ---------- copy ----------

    @Test
    void copy_readyTemplate_createsOwnCopyWithSameExercises() {
        when(templateRepository.findWithExercisesById(templateId)).thenReturn(Optional.of(readyTemplate()));
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutTemplateResponse response = service.copy(me, templateId);

        ArgumentCaptor<WorkoutTemplate> saved = ArgumentCaptor.forClass(WorkoutTemplate.class);
        verify(templateRepository).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(me);
        assertThat(response.name()).isEqualTo("Ноги");
        assertThat(response.exercises()).extracting(TemplateExerciseResponse::name).containsExactly("Приседания со штангой", "Жим лёжа");
    }

    // ---------- helpers ----------

    private static WorkoutTemplateRequest request(String name, UUID... exerciseIds) {
        return new WorkoutTemplateRequest(name, "violet", "steps", List.of(exerciseIds));
    }

    private WorkoutTemplate readyTemplate() {
        WorkoutTemplate template = WorkoutTemplate.custom(UUID.randomUUID(), "Ноги", "violet", "steps", List.of(squat, bench));
        ReflectionTestUtils.setField(template, "ownerId", null);
        return template;
    }

    private static Exercise systemExercise(String name) {
        Exercise exercise = exercise(UUID.randomUUID(), name);
        ReflectionTestUtils.setField(exercise, "ownerId", null);
        return exercise;
    }

    /** В тестах нет БД, поэтому id проставляем сами. */
    private static Exercise exercise(UUID ownerId, String name) {
        Exercise exercise = Exercise.custom(ownerId, name, BodyPart.LEGS, Equipment.BARBELL, TrackingType.WEIGHT_REPS, null, null);
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        return exercise;
    }
}
