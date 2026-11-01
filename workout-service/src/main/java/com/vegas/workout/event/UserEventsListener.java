package com.vegas.workout.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.common.event.DemoDataRequestedEvent;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.UserDeletedEvent;
import com.vegas.workout.demo.DemoDataGenerator;
import com.vegas.workout.service.UserDataCleaner;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Читает user-events: нужен только UserDeleted. Своя группа "workout-service" — отдельно от
 * "workout-service.trainer-links" (common-trainer-access), иначе Kafka поделила бы между ними партиции.
 */
@Component
@RequiredArgsConstructor
public class UserEventsListener {

    private final UserDataCleaner cleaner;
    private final DemoDataGenerator demoDataGenerator;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.user-events:user-events}", groupId = "${spring.application.name}",
            properties = "auto.offset.reset=earliest")
    public void onUserEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Header header = record.headers().lastHeader(EventTypes.HEADER);
        String eventType = header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
        if (EventTypes.USER_DELETED.equals(eventType)) {
            cleaner.deleteAll(objectMapper.readValue(record.value(), UserDeletedEvent.class).userId());
        } else if (EventTypes.DEMO_DATA_REQUESTED.equals(eventType)) {
            // демо "посмотреть изнутри" (docs/demo.md): история тренировок по сценарию
            demoDataGenerator.generate(objectMapper.readValue(record.value(), DemoDataRequestedEvent.class));
        }
    }
}
