package com.vegas.workout.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.model.BodyPart;
import com.vegas.workout.dto.ProgramAlternativeResponse;
import com.vegas.workout.dto.ProgramRequest;
import com.vegas.workout.dto.ProgramResponse;
import com.vegas.workout.dto.ProgramSwapRequest;
import com.vegas.workout.dto.WeeklyPlanRequest;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.ExerciseExclusion;
import com.vegas.workout.entity.ExperienceLevel;
import com.vegas.workout.entity.Program;
import com.vegas.workout.entity.ProgramAnswers;
import com.vegas.workout.entity.ProgramGoal;
import com.vegas.workout.entity.ProgramLocation;
import com.vegas.workout.entity.ProgramSlot;
import com.vegas.workout.entity.ProgramStatus;
import com.vegas.workout.entity.SetScheme;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.entity.WorkoutStatus;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.mapper.ProgramMapper;
import com.vegas.workout.program.GeneratedProgram;
import com.vegas.workout.program.ProgramGenerator;
import com.vegas.workout.repository.ExerciseExclusionRepository;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.ProgramRepository;
import com.vegas.workout.repository.WeeklyPlanDayRepository;
import com.vegas.workout.repository.WorkoutExerciseRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Сервис программы: склейка генератора с базой. Правила самого подбора проверяет ProgramGeneratorTest,
 * правила замены в сущности — ProgramTest; здесь — что сервис правильно собирает и сохраняет.
 * LENIENT: общие заглушки из setUp нужны не каждому тесту — строгий Mockito ругался бы на «лишние».
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgramServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    @Mock private ProgramRepository programRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private ExerciseExclusionRepository exclusionRepository;
    @Mock private WorkoutExerciseRepository workoutExerciseRepository;
    @Mock private WorkoutTemplateRepository templateRepository;
    @Mock private WeeklyPlanDayRepository planRepository;
    @Mock private WeeklyPlanService weeklyPlanService;
    @Mock private ProgramGenerator generator;

    private ProgramService service;
    private final UUID me = UUID.randomUUID();
    private final List<Exercise> catalog = new ArrayList<>();

    private Exercise benchPress;
    private Exercise dumbbellPress;
    private Exercise machinePress;
    private Exercise squat;

    @BeforeEach
    void setUp() {
        service = new ProgramService(programRepository, exerciseRepository, exclusionRepository,
                workoutExerciseRepository, templateRepository, planRepository, weeklyPlanService, generator,
                new ProgramMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        benchPress = exercise("Жим штанги лёжа", Equipment.BARBELL, "HORIZONTAL_PUSH");
        dumbbellPress = exercise("Жим гантелей лёжа", Equipment.DUMBBELL, "HORIZONTAL_PUSH");
        machinePress = exercise("Жим в тренажёре", Equipment.MACHINE, "HORIZONTAL_PUSH");
        squat = exercise("Приседания со штангой", Equipment.BARBELL, "SQUAT");

        when(exerciseRepository.findAllByOwnerIdIsNullAndArchivedAtIsNull()).thenReturn(catalog);
        when(exclusionRepository.findExerciseIdsByOwnerId(me)).thenReturn(Set.of());
        when(workoutExerciseRepository.findDoneExerciseIds(me, WorkoutStatus.COMPLETED)).thenReturn(Set.of());
        when(planRepository.findByUserId(me)).thenReturn(List.of());
        // save комплекса: как база — выдаём id
        when(templateRepository.save(any())).thenAnswer(inv -> {
            WorkoutTemplate t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });
    }

    // ---------- создание ----------

    @Test
    void create_archivesOldProgramAndFlushesBeforeSavingNew() {
        Program old = program(dayWith(benchPress, squat));
        when(programRepository.findActive(me)).thenReturn(Optional.of(old));
        when(generator.generate(any(), any(), any(), any())).thenReturn(oneDay(benchPress, squat));

        service.create(me, request());

        assertThat(old.getStatus()).isEqualTo(ProgramStatus.ARCHIVED);
        // частичный уникальный индекс «одна ACTIVE на человека»: UPDATE старой обязан уйти раньше INSERT новой
        InOrder order = inOrder(programRepository);
        order.verify(programRepository).flush();
        order.verify(programRepository).save(any(Program.class));
    }

    @Test
    void create_daysBecomeTemplates_slotsKeepOrder_planReplaced() {
        when(programRepository.findActive(me)).thenReturn(Optional.empty());
        when(generator.generate(any(), any(), any(), any())).thenReturn(oneDay(benchPress, squat));

        ProgramResponse response = service.create(me, request());

        ArgumentCaptor<Program> saved = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).save(saved.capture());
        assertThat(saved.getValue().getSlots()).extracting(ProgramSlot::getPosition).containsExactly(0, 1);
        assertThat(saved.getValue().getSlots()).extracting(s -> s.getExercise().getName())
                .containsExactly("Жим штанги лёжа", "Приседания со штангой");
        assertThat(saved.getValue().getSlots().get(0).getTemplate().getExercises()).containsExactly(benchPress, squat);

        ArgumentCaptor<WeeklyPlanRequest> plan = ArgumentCaptor.forClass(WeeklyPlanRequest.class);
        verify(weeklyPlanService).replace(eq(me), plan.capture());
        assertThat(plan.getValue().days()).extracting(WeeklyPlanRequest.Day::dayOfWeek)
                .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
        assertThat(response.notes()).containsExactly("Заметка");
    }

    @Test
    void create_nameTaken_addsNumber() {
        when(programRepository.findActive(me)).thenReturn(Optional.empty());
        when(generator.generate(any(), any(), any(), any())).thenReturn(oneDay(benchPress));
        when(templateRepository.existsByOwnerIdAndNameIgnoreCase(me, "День A · всё тело")).thenReturn(true);
        when(templateRepository.existsByOwnerIdAndNameIgnoreCase(me, "День A · всё тело (2)")).thenReturn(false);

        service.create(me, request());

        ArgumentCaptor<WorkoutTemplate> template = ArgumentCaptor.forClass(WorkoutTemplate.class);
        verify(templateRepository).save(template.capture());
        assertThat(template.getValue().getName()).isEqualTo("День A · всё тело (2)");
    }

    // ---------- замена ----------

    @Test
    void alternatives_sameFamilyOnly_notAlreadyInDay_familiarFirst() {
        Program program = program(dayWith(benchPress, squat));
        when(programRepository.findActive(me)).thenReturn(Optional.of(program));
        when(workoutExerciseRepository.findDoneExerciseIds(me, WorkoutStatus.COMPLETED))
                .thenReturn(Set.of(machinePress.getId()));
        when(workoutExerciseRepository.findRecent(eq(me), any(), any(), any())).thenReturn(List.of());

        List<ProgramAlternativeResponse> result = service.alternatives(me, slotOf(program, benchPress).getId());

        // «Жим в тренажёре» знаком → первым; штанги нет (стоит сейчас), приседа нет (другое семейство)
        assertThat(result).extracting(ProgramAlternativeResponse::name)
                .containsExactly("Жим в тренажёре", "Жим гантелей лёжа");
        assertThat(result.get(0).familiar()).isTrue();
    }

    @Test
    void swap_replacesInSlotAndInTemplate_samePosition_andExcludesOld() {
        Program program = program(dayWith(benchPress, squat));
        when(programRepository.findActive(me)).thenReturn(Optional.of(program));
        ProgramSlot slot = slotOf(program, benchPress);

        service.swap(me, slot.getId(), new ProgramSwapRequest(dumbbellPress.getId(), true));

        assertThat(slot.getExercise()).isEqualTo(dumbbellPress);
        assertThat(slot.isSwapped()).isTrue();
        assertThat(slot.getTemplate().getExercises()).containsExactly(dumbbellPress, squat);
        ArgumentCaptor<ExerciseExclusion> exclusion = ArgumentCaptor.forClass(ExerciseExclusion.class);
        verify(exclusionRepository).save(exclusion.capture());
        assertThat(exclusion.getValue().getExerciseId()).isEqualTo(benchPress.getId());
    }

    @Test
    void swap_otherFamily_rejected_nothingChanged() {
        Program program = program(dayWith(benchPress, squat));
        when(programRepository.findActive(me)).thenReturn(Optional.of(program));
        ProgramSlot slot = slotOf(program, benchPress);

        assertThatThrownBy(() -> service.swap(me, slot.getId(), new ProgramSwapRequest(squat.getId(), false)))
                .isInstanceOf(BadRequestException.class);
        assertThat(slot.getExercise()).isEqualTo(benchPress);
        verify(exclusionRepository, never()).save(any());
    }

    @Test
    void swap_secondTime_rejected() {
        Program program = program(dayWith(benchPress, squat));
        when(programRepository.findActive(me)).thenReturn(Optional.of(program));
        ProgramSlot slot = slotOf(program, benchPress);
        service.swap(me, slot.getId(), new ProgramSwapRequest(dumbbellPress.getId(), false));

        assertThatThrownBy(() -> service.swap(me, slot.getId(), new ProgramSwapRequest(machinePress.getId(), false)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.alternatives(me, slot.getId()))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- помощники ----------

    private static ProgramRequest request() {
        return new ProgramRequest(ProgramGoal.MUSCLE, 2, 60, ProgramLocation.GYM, ExperienceLevel.BEGINNER, null, null);
    }

    private static GeneratedProgram oneDay(Exercise... exercises) {
        List<GeneratedProgram.Slot> slots = new ArrayList<>();
        for (Exercise e : exercises) {
            slots.add(new GeneratedProgram.Slot(e, e.getMovementPattern(), SetScheme.of(3, 8, 12, 120), true));
        }
        return new GeneratedProgram("Всё тело × 2", List.of(new GeneratedProgram.Day("День A · всё тело", slots)),
                Map.of(DayOfWeek.MONDAY, 0, DayOfWeek.THURSDAY, 0), List.of("Заметка"));
    }

    private WorkoutTemplate dayWith(Exercise... exercises) {
        WorkoutTemplate template = WorkoutTemplate.custom(me, "День A", "orange", null, new ArrayList<>(List.of(exercises)));
        ReflectionTestUtils.setField(template, "id", UUID.randomUUID());
        return template;
    }

    /** Активная программа из одного дня; слотам выдаём id, как это сделала бы база */
    private Program program(WorkoutTemplate day) {
        ProgramAnswers answers = request().toAnswers();
        Program program = Program.start(me, answers, List.of(), NOW, LocalDate.of(2026, 11, 14));
        List<Exercise> exercises = day.getExercises();
        for (int i = 0; i < exercises.size(); i++) {
            ProgramSlot slot = program.addSlot(day, i, exercises.get(i), SetScheme.of(3, 8, 12, 120));
            ReflectionTestUtils.setField(slot, "id", UUID.randomUUID());
        }
        return program;
    }

    private static ProgramSlot slotOf(Program program, Exercise exercise) {
        return program.getSlots().stream().filter(s -> s.getExercise().equals(exercise)).findFirst().orElseThrow();
    }

    private Exercise exercise(String name, Equipment equipment, String family) {
        Exercise exercise = Exercise.custom(UUID.randomUUID(), name, BodyPart.CHEST, equipment,
                TrackingType.WEIGHT_REPS, null, null);
        ReflectionTestUtils.setField(exercise, "id", UUID.nameUUIDFromBytes(name.getBytes()));
        ReflectionTestUtils.setField(exercise, "ownerId", null);
        ReflectionTestUtils.setField(exercise, "movementPattern", family);
        catalog.add(exercise);
        return exercise;
    }
}
