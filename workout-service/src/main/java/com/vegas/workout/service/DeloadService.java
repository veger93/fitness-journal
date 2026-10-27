package com.vegas.workout.service;

import com.vegas.common.exception.NotFoundException;
import com.vegas.workout.dto.DeloadRequest;
import com.vegas.workout.dto.DeloadResponse;
import com.vegas.workout.entity.DeloadPeriod;
import com.vegas.workout.repository.DeloadPeriodRepository;
import com.vegas.workout.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Режим разгрузки по упражнению. План на неделю не трогаем: разгрузка — это подсказка в подходе
 * "70 кг × 5, 3 подхода" вместо обычной прогрессии, где бы упражнение ни встретилось.
 */
@Service
@RequiredArgsConstructor
public class DeloadService {

    private final DeloadPeriodRepository deloadRepository;
    private final ExerciseRepository exerciseRepository;
    private final Clock clock;

    /** Начать разгрузку. Уже идёт другая по этому упражнению — заканчиваем её: активной может быть только одна. */
    @Transactional
    public DeloadResponse start(UUID userId, UUID exerciseId, DeloadRequest request) {
        exerciseRepository.findById(exerciseId)
                .filter(exercise -> exercise.isVisibleTo(userId))
                .orElseThrow(() -> new NotFoundException("Упражнение не найдено"));

        Instant now = clock.instant();
        deloadRepository.findByUserIdAndExerciseIdAndEndsAtAfter(userId, exerciseId, now)
                .forEach(active -> active.endAt(now));

        DeloadPeriod period = new DeloadPeriod(userId, exerciseId, request.workingWeightKg(), request.sets(),
                request.reps(), now, now.plus(Duration.ofDays(request.days())));
        return DeloadResponse.from(deloadRepository.save(period));
    }

    /** Закончить раньше. Нет активной — ничего не делаем: повторный DELETE не ошибка (идемпотентность). */
    @Transactional
    public void stop(UUID userId, UUID exerciseId) {
        Instant now = clock.instant();
        deloadRepository.findByUserIdAndExerciseIdAndEndsAtAfter(userId, exerciseId, now)
                .forEach(active -> active.endAt(now));
    }

    @Transactional(readOnly = true)
    public List<DeloadResponse> active(UUID userId) {
        return deloadRepository.findByUserIdAndEndsAtAfter(userId, clock.instant()).stream()
                .map(DeloadResponse::from)
                .toList();
    }

    /** exerciseId -> активная разгрузка: для подсказок в идущей тренировке. */
    @Transactional(readOnly = true)
    public Map<UUID, DeloadResponse> activeByExercise(UUID userId) {
        return active(userId).stream()
                .collect(Collectors.toMap(DeloadResponse::exerciseId, Function.identity(), (a, b) -> a));
    }
}
