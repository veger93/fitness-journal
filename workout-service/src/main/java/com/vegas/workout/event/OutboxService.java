package com.vegas.workout.event;

import com.vegas.common.event.EventTypes;
import com.vegas.workout.entity.Workout;
import com.vegas.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Какие события тренировок пишутся в outbox (саму запись делает OutboxWriter из common-outbox).
 *
 * Propagation.MANDATORY: метод ОБЯЗАН вызываться внутри уже открытой транзакции
 * (иначе исключение). Весь смысл outbox — событие и бизнес-изменение коммитятся вместе
 * или откатываются вместе. Вызов вне транзакции — это ошибка программиста, ловим её сразу.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxWriter outboxWriter;

    @Transactional(propagation = Propagation.MANDATORY)
    public void workoutCompleted(Workout workout) {
        save(workout.getId(), EventTypes.WORKOUT_COMPLETED, workout.getUserId(), WorkoutEvents.completed(workout));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void workoutDeleted(Workout workout) {
        save(workout.getId(), EventTypes.WORKOUT_DELETED, workout.getUserId(), WorkoutEvents.deleted(workout));
    }

    /** Ключ сообщения = userId: все события одного пользователя попадут в одну партицию и придут по порядку. */
    private void save(UUID aggregateId, String eventType, UUID userId, Object event) {
        outboxWriter.write(aggregateId, eventType, userId.toString(), event);
    }
}
