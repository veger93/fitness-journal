package com.vegas.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutDeletedEvent;
import com.vegas.notification.service.WorkoutActivityService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WorkoutEventsListenerTest {

    @Mock
    private WorkoutActivityService activityService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private WorkoutEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new WorkoutEventsListener(activityService, objectMapper);
    }

    @Test
    void completed_isRecorded() throws Exception {
        WorkoutCompletedEvent event = new WorkoutCompletedEvent(UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-10-19T18:00:00Z"), List.of());

        listener.onWorkoutEvent(record(EventTypes.WORKOUT_COMPLETED, objectMapper.writeValueAsString(event)));

        verify(activityService).completed(event);
    }

    @Test
    void deleted_isRemoved() throws Exception {
        WorkoutDeletedEvent event = new WorkoutDeletedEvent(UUID.randomUUID(), UUID.randomUUID());

        listener.onWorkoutEvent(record(EventTypes.WORKOUT_DELETED, objectMapper.writeValueAsString(event)));

        verify(activityService).deleted(event);
    }

    @Test
    void otherTypes_areIgnored() throws Exception {
        listener.onWorkoutEvent(record("Unknown", "{}"));

        verifyNoInteractions(activityService);
    }

    private static ConsumerRecord<String, String> record(String eventType, String json) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("workout-events", 0, 0L, "key", json);
        record.headers().add(EventTypes.HEADER, eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
