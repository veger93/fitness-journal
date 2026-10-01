package com.vegas.workout.service;

import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.workout.dto.AddWorkoutExerciseRequest;
import com.vegas.workout.dto.PageResponse;
import com.vegas.workout.dto.SetRequest;
import com.vegas.workout.dto.SetResponse;
import com.vegas.workout.dto.StartWorkoutRequest;
import com.vegas.workout.dto.WorkoutResponse;
import com.vegas.workout.dto.WorkoutSummaryResponse;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.SetValues;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutSet;
import com.vegas.workout.entity.WorkoutStatus;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.mapper.WorkoutMapper;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.WorkoutExerciseRepository;
import com.vegas.workout.repository.WorkoutRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private static final String DEFAULT_NAME = "Тренировка";

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutTemplateRepository templateRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutMapper workoutMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ---------- тренировка ----------

    @Transactional
    public WorkoutResponse start(UUID userId, StartWorkoutRequest request) {
        if (workoutRepository.existsByUserIdAndStatus(userId, WorkoutStatus.IN_PROGRESS)) {
            // фронт в ответ может предложить "продолжить текущую" (GET /api/workouts/current)
            throw new BusinessException("У вас уже есть незавершённая тренировка");
        }

        Workout workout;
        if (request.templateId() != null) {
            WorkoutTemplate template = templateRepository.findWithExercisesById(request.templateId())
                    .filter(found -> found.isVisibleTo(userId))
                    .orElseThrow(() -> new NotFoundException("Комплекс не найден"));
            workout = Workout.start(userId, nameOrDefault(request.name(), template.getName()),
                    template.getId(), template.getExercises(), clock.instant());
        } else {
            workout = Workout.start(userId, nameOrDefault(request.name(), DEFAULT_NAME),
                    null, List.of(), clock.instant());
        }
        return toResponse(workoutRepository.save(workout));
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutResponse> getCurrent(UUID userId) {
        return workoutRepository.findWithExercisesByUserIdAndStatus(userId, WorkoutStatus.IN_PROGRESS)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkoutResponse getById(UUID userId, UUID workoutId) {
        return toResponse(findOwn(userId, workoutId));
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkoutSummaryResponse> history(UUID userId, int page, int size) {
        return PageResponse.of(workoutRepository.findHistory(
                userId, WorkoutStatus.COMPLETED, PageRequest.of(page, size)));
    }

    @Transactional
    public WorkoutResponse complete(UUID userId, UUID workoutId) {
        Workout workout = findOwn(userId, workoutId);
        workout.complete(clock.instant());
        // Событие уйдёт слушателям только ПОСЛЕ успешного коммита (см. WorkoutEventListener)
        eventPublisher.publishEvent(new WorkoutCompletedEvent(workout.getId(), userId, workout.getCompletedAt()));
        return toResponse(workout);
    }

    @Transactional
    public void delete(UUID userId, UUID workoutId) {
        workoutRepository.delete(findOwn(userId, workoutId));
    }

    // ---------- упражнения в тренировке ----------

    @Transactional
    public WorkoutResponse addExercise(UUID userId, UUID workoutId, AddWorkoutExerciseRequest request) {
        Workout workout = findOwn(userId, workoutId);
        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .filter(found -> found.isVisibleTo(userId))
                .orElseThrow(() -> new BadRequestException("Упражнение не найдено"));

        workout.addExercise(exercise);
        // flush: отправить изменения в БД сейчас, чтобы у нового упражнения появился id для ответа
        workoutRepository.flush();
        return toResponse(workout);
    }

    @Transactional
    public WorkoutResponse removeExercise(UUID userId, UUID workoutId, UUID workoutExerciseId) {
        Workout workout = findOwn(userId, workoutId);
        workout.removeExercise(workoutExerciseId);
        workoutRepository.flush();
        return toResponse(workout);
    }

    // ---------- подходы ----------

    @Transactional
    public SetResponse addSet(UUID userId, UUID workoutId, UUID workoutExerciseId, SetRequest request) {
        WorkoutExercise workoutExercise = findOwn(userId, workoutId).findExercise(workoutExerciseId);
        SetValues values = toSetValues(workoutExercise.getExercise().getTrackingType(), request);

        WorkoutSet set = workoutExercise.addSet(values, clock.instant());
        workoutRepository.flush();
        return workoutMapper.toSetResponse(set);
    }

    @Transactional
    public SetResponse updateSet(UUID userId, UUID workoutId, UUID workoutExerciseId, UUID setId, SetRequest request) {
        WorkoutExercise workoutExercise = findOwn(userId, workoutId).findExercise(workoutExerciseId);
        SetValues values = toSetValues(workoutExercise.getExercise().getTrackingType(), request);

        WorkoutSet set = workoutExercise.findSet(setId);
        set.update(values);
        return workoutMapper.toSetResponse(set);
    }

    @Transactional
    public void deleteSet(UUID userId, UUID workoutId, UUID workoutExerciseId, UUID setId) {
        findOwn(userId, workoutId).findExercise(workoutExerciseId).removeSet(setId);
    }

    // ---------- вспомогательное ----------

    /** Чужая тренировка ищется так же, как несуществующая -> 404. */
    private Workout findOwn(UUID userId, UUID workoutId) {
        return workoutRepository.findWithExercisesByIdAndUserId(workoutId, userId)
                .orElseThrow(() -> new NotFoundException("Тренировка не найдена"));
    }

    private WorkoutResponse toResponse(Workout workout) {
        Map<UUID, List<WorkoutSet>> previous = workout.isInProgress() ? findPreviousSets(workout) : Map.of();
        return workoutMapper.toResponse(workout, previous);
    }

    /**
     * Для каждого упражнения — подходы из последней завершённой тренировки с ним.
     * По запросу на упражнение: в тренировке их обычно 3–8, это приемлемо.
     */
    private Map<UUID, List<WorkoutSet>> findPreviousSets(Workout workout) {
        Map<UUID, List<WorkoutSet>> result = new HashMap<>();
        for (WorkoutExercise workoutExercise : workout.getExercises()) {
            UUID exerciseId = workoutExercise.getExercise().getId();
            workoutExerciseRepository
                    .findRecent(workout.getUserId(), exerciseId, WorkoutStatus.COMPLETED, Limit.of(1))
                    .stream()
                    .findFirst()
                    .ifPresent(last -> result.put(exerciseId, last.getSets()));
        }
        return result;
    }

    /** Какие поля подхода обязательны — зависит от того, что записываем в упражнении. */
    private static SetValues toSetValues(TrackingType trackingType, SetRequest request) {
        switch (trackingType) {
            case WEIGHT_REPS -> require(request.reps() != null, "Укажите количество повторов");
            case TIME -> require(request.durationSec() != null, "Укажите время");
            case DISTANCE -> require(request.distanceM() != null, "Укажите дистанцию");
        }
        return new SetValues(request.weightKg(), request.reps(), request.durationSec(),
                request.distanceM(), request.rpe(), Boolean.TRUE.equals(request.warmup()));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new BadRequestException(message);
        }
    }

    private static String nameOrDefault(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name.trim();
    }
}
