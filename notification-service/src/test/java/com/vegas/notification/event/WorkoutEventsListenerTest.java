package com.vegas.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.ProgramChangedEvent;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WeeklyPlanUpdatedEvent;
import com.vegas.common.event.WorkoutDeletedEvent;
import com.vegas.notification.service.ProgramReviewSyncService;
import com.vegas.notification.service.WeeklyPlanSyncService;
import com.vegas.notification.service.WorkoutActivityService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WorkoutEventsListenerTest {

    @Mock
    private WorkoutActivityService activityService;
    @Mock
    private WeeklyPlanSyncService planService;
    @Mock
    private ProgramReviewSyncService programReviewService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private WorkoutEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new WorkoutEventsListener(activityService, planService, programReviewService, objectMapper);
    }

    @Test
    void completed_isRecorded() throws Exception {
        WorkoutCompletedEvent event = new WorkoutCompletedEvent(UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-10-19T18:00:00Z"), List.of());

        listener.onWorkoutEvent(record(EventTypes.WORKOUT_COMPLETED, objectMapper.writeValueAsString(event)));

        verify(activityService).completed(event);
    }

    @Test
    void programChanged_isApplied_withNullsForNoProgram() throws Exception {
        ProgramChangedEvent started = ProgramChangedEvent.started(UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 12, 26), Instant.parse("2026-11-14T10:00:00Z"));
        ProgramChangedEvent none = ProgramChangedEvent.none(UUID.randomUUID(), Instant.parse("2026-11-15T10:00:00Z"));

        listener.onWorkoutEvent(record(EventTypes.PROGRAM_CHANGED, objectMapper.writeValueAsString(started)));
        listener.onWorkoutEvent(record(EventTypes.PROGRAM_CHANGED, objectMapper.writeValueAsString(none)));

        verify(programReviewService).apply(started);
        verify(programReviewService).apply(none);
    }

    @Test
    void deleted_isRemoved() throws Exception {
        WorkoutDeletedEvent event = new WorkoutDeletedEvent(UUID.randomUUID(), UUID.randomUUID());

        listener.onWorkoutEvent(record(EventTypes.WORKOUT_DELETED, objectMapper.writeValueAsString(event)));

        verify(activityService).deleted(event);
    }

    @Test
    void weeklyPlan_isApplied() throws Exception {
        WeeklyPlanUpdatedEvent event = new WeeklyPlanUpdatedEvent(UUID.randomUUID(),
                List.of(new WeeklyPlanUpdatedEvent.PlannedDay(java.time.DayOfWeek.MONDAY, "Верх")),
                Instant.parse("2026-10-19T18:00:00Z"));

        listener.onWorkoutEvent(record(EventTypes.WEEKLY_PLAN_UPDATED, objectMapper.writeValueAsString(event)));

        // JSON туда и обратно даёт равный record — значит, контракт события сериализуется без потерь
        verify(planService).apply(event);
    }

    @Test
    void otherTypes_areIgnored() throws Exception {
        listener.onWorkoutEvent(record("Unknown", "{}"));

        verifyNoInteractions(activityService, planService);
    }

    private static ConsumerRecord<String, String> record(String eventType, String json) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("workout-events", 0, 0L, "key", json);
        record.headers().add(EventTypes.HEADER, eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
