package com.vegas.analytics.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.analytics.service.AthleteProfileService;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.UserProfileUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Читает топик user-events (профиль пользователя). Устроен так же, как WorkoutEventsListener. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventsListener {

    private final AthleteProfileService athleteProfileService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.user-events}")
    public void onUserEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Header header = record.headers().lastHeader(EventTypes.HEADER);
        String eventType = header == null ? null : new String(header.value(), StandardCharsets.UTF_8);

        if (EventTypes.USER_PROFILE_UPDATED.equals(eventType)) {
            athleteProfileService.apply(objectMapper.readValue(record.value(), UserProfileUpdatedEvent.class));
        } else {
            log.debug("User event type {} is not handled by analytics", eventType);
        }
    }
}
