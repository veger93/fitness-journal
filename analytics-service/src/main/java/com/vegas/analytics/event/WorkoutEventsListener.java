package com.vegas.analytics.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.analytics.service.PerformanceService;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Читает топик workout-events. Тип события — в заголовке eventType,
 * по нему выбираем, в какой класс разобрать JSON и что делать.
 *
 * Метод вернулся без исключения -> Spring фиксирует offset (сообщение обработано).
 * Бросил исключение -> повторы, затем DLT (см. KafkaConsumerConfig).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutEventsListener {

    private final PerformanceService performanceService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.workout-events}")
    public void onWorkoutEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        String eventType = headerValue(record, EventTypes.HEADER);
        if (eventType == null) {
            log.warn("Message without {} header skipped: partition={}, offset={}",
                    EventTypes.HEADER, record.partition(), record.offset());
            return;
        }

        switch (eventType) {
            case EventTypes.WORKOUT_COMPLETED ->
                    performanceService.recordWorkout(objectMapper.readValue(record.value(), WorkoutCompletedEvent.class));
            case EventTypes.WORKOUT_DELETED ->
                    performanceService.deleteWorkout(objectMapper.readValue(record.value(), WorkoutDeletedEvent.class));
            // новые типы событий, о которых этот сервис пока не знает, просто пропускаем
            default -> log.debug("Event type {} is not handled by analytics", eventType);
        }
    }

    private static String headerValue(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
