package com.vegas.analytics.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vegas.analytics.service.PerformanceService;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutDeletedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WorkoutEventsListenerTest {

    @Mock
    private PerformanceService performanceService;

    // такой же ObjectMapper, как настраивает Spring Boot (с поддержкой Instant)
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private WorkoutEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new WorkoutEventsListener(performanceService, objectMapper);
    }

    @Test
    void completedEvent_isRecorded() throws Exception {
        WorkoutCompletedEvent event = new WorkoutCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-10-01T11:00:00Z"), List.of());

        listener.onWorkoutEvent(record(EventTypes.WORKOUT_COMPLETED, objectMapper.writeValueAsString(event)));

        ArgumentCaptor<WorkoutCompletedEvent> received = ArgumentCaptor.forClass(WorkoutCompletedEvent.class);
        verify(performanceService).recordWorkout(received.capture());
        assertThat(received.getValue()).isEqualTo(event); // record: equals по всем полям
    }

    @Test
    void deletedEvent_isRemoved() throws Exception {
        WorkoutDeletedEvent event = new WorkoutDeletedEvent(UUID.randomUUID(), UUID.randomUUID());

        listener.onWorkoutEvent(record(EventTypes.WORKOUT_DELETED, objectMapper.writeValueAsString(event)));

        verify(performanceService).deleteWorkout(event);
    }

    @Test
    void unknownEventType_isIgnored() throws Exception {
        listener.onWorkoutEvent(record("SomethingNew", "{}"));

        verifyNoInteractions(performanceService);
    }

    @Test
    void brokenJson_throws_soErrorHandlerSendsItToDlt() {
        assertThatThrownBy(() -> listener.onWorkoutEvent(record(EventTypes.WORKOUT_COMPLETED, "{not json")))
                .isInstanceOf(JsonProcessingException.class);
    }

    private static ConsumerRecord<String, String> record(String eventType, String json) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("workout-events", 0, 0L, "user-key", json);
        record.headers().add(EventTypes.HEADER, eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
