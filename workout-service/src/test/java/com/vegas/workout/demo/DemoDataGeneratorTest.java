package com.vegas.workout.demo;

import com.vegas.common.event.DemoDataRequestedEvent;
import com.vegas.common.model.BodyPart;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutStatus;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.event.OutboxService;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.WorkoutRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import com.vegas.workout.service.WeeklyPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    @Mock
    private WorkoutRepository workoutRepository;
    @Mock
    private WorkoutTemplateRepository templateRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private WeeklyPlanService weeklyPlanService;
    @Mock
    private OutboxService outboxService;

    private DemoDataGenerator generator;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        generator = new DemoDataGenerator(workoutRepository, templateRepository, exerciseRepository, weeklyPlanService,
                outboxService, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(1));
        // любое название из каталога находится
        lenient().when(exerciseRepository.findFirstByOwnerIdIsNullAndNameIgnoreCaseAndArchivedAtIsNull(anyString()))
                .thenAnswer(inv -> Optional.of(exercise(inv.getArgument(0))));
        lenient().when(templateRepository.save(any(WorkoutTemplate.class))).thenAnswer(inv -> {
            WorkoutTemplate template = inv.getArgument(0);
            ReflectionTestUtils.setField(template, "id", UUID.randomUUID());
            return template;
        });
    }

    @Test
    void athlete_completedWorkoutsWithEvents_templatesAndWeeklyPlan() {
        int created = generator.generate(new DemoDataRequestedEvent(user, "ATHLETE", null, NOW.minusSeconds(5)));

        int planned = DemoScenarios.plan("ATHLETE", NOW).size();
        assertThat(created).isEqualTo(planned);
        ArgumentCaptor<Workout> saved = ArgumentCaptor.forClass(Workout.class);
        verify(workoutRepository, times(planned)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(w -> {
            assertThat(w.getStatus()).isEqualTo(WorkoutStatus.COMPLETED);
            assertThat(w.getUserId()).isEqualTo(user);
            assertThat(w.getRecordedBy()).isNull();
            assertThat(w.getCompletedAt()).isBefore(NOW);
        });
        // каждая тренировка — обычное событие для аналитики
        verify(outboxService, times(planned)).workoutCompleted(any());
        verify(templateRepository, times(3)).save(any());
        verify(weeklyPlanService).replace(eq(user), any());
    }

    @Test
    void client_recordedByTrainer_inProgressWithoutEvent() {
        UUID coach = UUID.randomUUID();
        generator.generate(new DemoDataRequestedEvent(user, "CLIENT_IN_PROGRESS", coach, NOW.minusSeconds(5)));

        ArgumentCaptor<Workout> saved = ArgumentCaptor.forClass(Workout.class);
        verify(workoutRepository, atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(w -> assertThat(w.getRecordedBy()).isEqualTo(coach));
        List<Workout> running = saved.getAllValues().stream().filter(Workout::isInProgress).toList();
        assertThat(running).hasSize(1);
        assertThat(running.get(0).getStartedAt()).isAfter(NOW.minus(Duration.ofHours(1)));
        // событие "завершена" — только у завершённых
        verify(outboxService, times(saved.getAllValues().size() - 1)).workoutCompleted(any());
        // у клиентов комплексов и плана нет — тренер ведёт их без шаблонов
        verify(weeklyPlanService, never()).replace(any(), any());
    }

    @Test
    void repeatedEvent_userAlreadyHasWorkouts_skipped() {
        when(workoutRepository.existsByUserId(user)).thenReturn(true);

        assertThat(generator.generate(new DemoDataRequestedEvent(user, "ATHLETE", null, NOW))).isZero();
        verify(workoutRepository, never()).save(any());
        verify(outboxService, never()).workoutCompleted(any());
    }

    @Test
    void oldEvent_skipped_noResurrectionOfDeletedDemo() {
        assertThat(generator.generate(new DemoDataRequestedEvent(user, "ATHLETE", null, NOW.minus(Duration.ofDays(2)))))
                .isZero();
        verify(workoutRepository, never()).existsByUserId(any());
        verify(workoutRepository, never()).save(any());
    }

    @Test
    void exerciseMissingFromCatalog_skipped_restCreated() {
        when(exerciseRepository.findFirstByOwnerIdIsNullAndNameIgnoreCaseAndArchivedAtIsNull("Бег"))
                .thenReturn(Optional.empty());

        int created = generator.generate(new DemoDataRequestedEvent(user, "ATHLETE", null, NOW));

        // пробежки (только "Бег") не создались, остальное — да
        long runs = DemoScenarios.plan("ATHLETE", NOW).stream().filter(s -> s.name().equals("Пробежка")).count();
        assertThat(created).isEqualTo(DemoScenarios.plan("ATHLETE", NOW).size() - runs);
    }

    private static Exercise exercise(String name) {
        Exercise exercise = Exercise.custom(UUID.randomUUID(), name, BodyPart.LEGS, Equipment.BARBELL,
                TrackingType.WEIGHT_REPS, null, null);
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(exercise, "ownerId", null);
        return exercise;
    }
}
