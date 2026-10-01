package com.vegas.workout.event;

import com.vegas.common.event.WorkoutCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Слушатель событий внутри сервиса (Spring Application Events).
 *
 * AFTER_COMMIT: метод вызовется, только если транзакция завершения УСПЕШНО закоммичена.
 * Если коммит упадёт, никто не узнает о "завершённой" тренировке, которой в БД нет.
 *
 * Позже здесь будет отправка события в Kafka для analytics-service
 * (пересчёт прогресса, поиск плато и рекордов).
 */
@Slf4j
@Component
public class WorkoutEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkoutCompleted(WorkoutCompletedEvent event) {
        log.info("Workout {} of user {} completed at {}", event.workoutId(), event.userId(), event.completedAt());
    }
}
