package com.vegas.notification.service;

import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutDeletedEvent;
import com.vegas.notification.entity.WorkoutActivity;
import com.vegas.notification.repository.WorkoutActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Локальная копия дат тренировок: для напоминаний не нужно ходить в workout-service. */
@Service
@RequiredArgsConstructor
public class WorkoutActivityService {

    private final WorkoutActivityRepository repository;

    /** Идемпотентно: повторное событие с тем же workoutId ничего не меняет. */
    @Transactional
    public void completed(WorkoutCompletedEvent event) {
        if (!repository.existsById(event.workoutId())) {
            repository.save(new WorkoutActivity(event.workoutId(), event.userId(), event.completedAt()));
        }
    }

    /** deleteById в Spring Data 3 не падает, если строки нет, — повтор события безопасен. */
    @Transactional
    public void deleted(WorkoutDeletedEvent event) {
        repository.deleteById(event.workoutId());
    }

    @Transactional(readOnly = true)
    public Optional<Instant> lastWorkoutAt(UUID userId) {
        return repository.findLastCompletedAt(userId);
    }
}
