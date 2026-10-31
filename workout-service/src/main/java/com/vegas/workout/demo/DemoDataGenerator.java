package com.vegas.workout.demo;

import com.vegas.common.event.DemoDataRequestedEvent;
import com.vegas.workout.dto.WeeklyPlanRequest;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.SetValues;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.event.OutboxService;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.WorkoutRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import com.vegas.workout.service.WeeklyPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Генератор тренировок демо-пользователя (docs/demo.md, шаг 2) по событию DemoDataRequested.
 *
 * Тренировки создаются обычными сущностями и уходят обычными WorkoutCompleted (outbox) — аналитика
 * сама посчитает рекорды, плато и перегрузку, как для настоящих тренировок.
 *
 * Всё одной транзакцией: либо история создана целиком вместе с событиями, либо ничего.
 * Повтор события (Kafka "минимум один раз") ничего не дублирует: у пользователя уже есть тренировки — пропускаем.
 * Старое событие (старше max-age) тоже пропускаем: если топик когда-нибудь перечитают с начала,
 * генератор не "воскресит" давно удалённых демо-пользователей.
 */
@Slf4j
@Service
public class DemoDataGenerator {

    private final WorkoutRepository workoutRepository;
    private final WorkoutTemplateRepository templateRepository;
    private final ExerciseRepository exerciseRepository;
    private final WeeklyPlanService weeklyPlanService;
    private final OutboxService outboxService;
    private final Clock clock;
    private final Duration maxAge;

    public DemoDataGenerator(WorkoutRepository workoutRepository,
                             WorkoutTemplateRepository templateRepository,
                             ExerciseRepository exerciseRepository,
                             WeeklyPlanService weeklyPlanService,
                             OutboxService outboxService,
                             Clock clock,
                             @Value("${app.demo.event-max-age:PT1H}") Duration maxAge) {
        this.workoutRepository = workoutRepository;
        this.templateRepository = templateRepository;
        this.exerciseRepository = exerciseRepository;
        this.weeklyPlanService = weeklyPlanService;
        this.outboxService = outboxService;
        this.clock = clock;
        this.maxAge = maxAge;
    }

    /** @return сколько тренировок создано (0 — пропустили) */
    @Transactional
    public int generate(DemoDataRequestedEvent event) {
        Instant now = clock.instant();
        if (event.requestedAt() == null || event.requestedAt().isBefore(now.minus(maxAge))) {
            log.warn("Demo data request for {} is too old ({}), skipped", event.userId(), event.requestedAt());
            return 0;
        }
        if (workoutRepository.existsByUserId(event.userId())) {
            log.info("Demo data for {} already exists, skipped", event.userId());
            return 0;
        }

        List<DemoScenarios.Session> plan = DemoScenarios.plan(event.scenario(), now);
        Map<String, Exercise> catalog = new HashMap<>();
        Map<String, UUID> templates = createTemplates(event.userId(), plan, catalog);

        int created = 0;
        for (DemoScenarios.Session session : plan) {
            if (createWorkout(event, session, templates, catalog)) {
                created++;
            }
        }
        planWeek(event.userId(), plan, templates);
        log.info("Demo data for {} ({}): {} workouts", event.userId(), event.scenario(), created);
        return created;
    }

    /** Комплексы — по первой тренировке с этим названием, в том же порядке упражнений */
    private Map<String, UUID> createTemplates(UUID userId, List<DemoScenarios.Session> plan, Map<String, Exercise> catalog) {
        Map<String, UUID> ids = new LinkedHashMap<>();
        String[][] looks = {{"blue", "pulldown"}, {"violet", "steps"}, {"orange", "dumbbell"}, {"green", "dumbbell"}};
        for (DemoScenarios.Session session : plan) {
            if (session.template() == null || ids.containsKey(session.template())) {
                continue;
            }
            List<Exercise> exercises = resolve(session.exercises(), catalog).stream().map(Resolved::exercise).toList();
            if (exercises.isEmpty()) {
                continue;
            }
            String[] look = looks[ids.size() % looks.length];
            WorkoutTemplate template = templateRepository.save(
                    WorkoutTemplate.custom(userId, session.template(), look[0], look[1], exercises));
            ids.put(session.template(), template.getId());
        }
        return ids;
    }

    /** false — тренировку не создали (в каталоге нет ни одного её упражнения или нет подходов) */
    private boolean createWorkout(DemoDataRequestedEvent event, DemoScenarios.Session session,
                                  Map<String, UUID> templates, Map<String, Exercise> catalog) {
        List<Resolved> exercises = resolve(session.exercises(), catalog);
        boolean hasSets = exercises.stream().anyMatch(r -> !r.sets().isEmpty());
        if (!hasSets) {
            return false;
        }
        Workout workout = Workout.create(event.userId(), session.name(), templates.get(session.template()),
                exercises.stream().map(Resolved::exercise).toList());
        workout.recordedBy(event.recordedBy());

        // подходы — каждые 3 минуты от начала: в истории видно реальное время и длительность
        Instant at = session.start();
        for (int i = 0; i < exercises.size(); i++) {
            WorkoutExercise workoutExercise = workout.getExercises().get(i);
            for (SetValues values : exercises.get(i).sets()) {
                at = at.plus(DemoScenarios.SET_EVERY);
                workoutExercise.addSet(values, at);
            }
        }
        if (!session.inProgress()) {
            workout.complete(session.end());
        }
        workoutRepository.save(workout);
        if (!session.inProgress()) {
            outboxService.workoutCompleted(workout);
        }
        return true;
    }

    /**
     * План на неделю: каждый комплекс — в тот день недели, когда он был в последний раз.
     * Через обычный сервис плана: он же отправит событие для напоминаний (у демо нет email — письма не уйдут).
     */
    private void planWeek(UUID userId, List<DemoScenarios.Session> plan, Map<String, UUID> templates) {
        if (templates.isEmpty()) {
            return;
        }
        Map<DayOfWeek, UUID> days = new LinkedHashMap<>();
        for (DemoScenarios.Session session : plan) {
            UUID templateId = templates.get(session.template());
            if (templateId != null) {
                days.put(session.start().atZone(ZoneOffset.UTC).getDayOfWeek(), templateId);
            }
        }
        List<WeeklyPlanRequest.Day> request = new ArrayList<>();
        days.forEach((day, templateId) -> request.add(new WeeklyPlanRequest.Day(day, templateId)));
        weeklyPlanService.replace(userId, new WeeklyPlanRequest(request));
    }

    private record Resolved(Exercise exercise, List<SetValues> sets) {
    }

    /** Названия -> упражнения каталога. Нет в каталоге (переименовали) — пропускаем, а не падаем */
    private List<Resolved> resolve(List<DemoScenarios.Exercise> exercises, Map<String, Exercise> catalog) {
        List<Resolved> resolved = new ArrayList<>();
        for (DemoScenarios.Exercise exercise : exercises) {
            Optional<Exercise> found = Optional.ofNullable(catalog.computeIfAbsent(exercise.name(),
                    name -> exerciseRepository.findFirstByOwnerIdIsNullAndNameIgnoreCaseAndArchivedAtIsNull(name).orElse(null)));
            if (found.isEmpty()) {
                log.warn("Demo exercise '{}' not found in catalog, skipped", exercise.name());
                continue;
            }
            resolved.add(new Resolved(found.get(), exercise.sets()));
        }
        return resolved;
    }
}
