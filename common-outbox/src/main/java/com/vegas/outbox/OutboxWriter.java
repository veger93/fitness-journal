package com.vegas.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Запись события в outbox — единственное, что нужно звать из бизнес-кода сервиса.
 *
 * Propagation.MANDATORY: ОБЯЗАТЕЛЬНО внутри уже открытой транзакции (иначе исключение).
 * Весь смысл outbox — событие и бизнес-изменение коммитятся вместе или откатываются вместе.
 */
public class OutboxWriter {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxWriter(OutboxEventRepository outboxRepository, ObjectMapper objectMapper, Clock clock) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * @param aggregateId id сущности, к которой относится событие (тренировка, пользователь)
     * @param eventType   тип из EventTypes — уйдёт в заголовок eventType
     * @param key         ключ Kafka-сообщения (обычно userId): одинаковый ключ -> одна партиция -> порядок
     * @param event       объект события, будет сериализован в JSON
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(UUID aggregateId, String eventType, String key, Object event) {
        outboxRepository.save(OutboxEvent.create(aggregateId, eventType, key, toJson(event), clock.instant()));
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + event.getClass().getSimpleName(), e);
        }
    }
}
