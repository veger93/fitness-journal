package com.vegas.notification.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.UserRegisteredEvent;
import com.vegas.notification.service.RecipientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Читает user-events: нужен только UserRegistered (email). UserProfileUpdated пропускаем. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventsListener {

    private final RecipientService recipientService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.user-events}")
    public void onUserEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        String eventType = EventHeaders.eventType(record);
        if (EventTypes.USER_REGISTERED.equals(eventType)) {
            recipientService.register(objectMapper.readValue(record.value(), UserRegisteredEvent.class));
        } else {
            log.debug("User event type {} is not handled by notifications", eventType);
        }
    }
}
