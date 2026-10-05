package com.vegas.workout.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.common.event.EventTypes;
import com.vegas.workout.entity.OutboxEvent;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Записывает события в outbox-таблицу.
 *
 * Propagation.MANDATORY: метод ОБЯЗАН вызываться внутри уже открытой транзакции
 * (иначе исключение). Весь смысл outbox — событие и бизнес-изменение коммитятся вместе
 * или откатываются вместе. Вызов вне транзакции — это ошибка программиста, ловим её сразу.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

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
        outboxRepository.save(OutboxEvent.create(aggregateId, eventType, userId.toString(), toJson(event), clock.instant()));
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + event.getClass().getSimpleName(), e);
        }
    }
}
