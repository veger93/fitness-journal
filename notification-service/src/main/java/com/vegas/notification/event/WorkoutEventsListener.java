package com.vegas.notification.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.WeeklyPlanUpdatedEvent;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutDeletedEvent;
import com.vegas.notification.service.WeeklyPlanSyncService;
import com.vegas.notification.service.WorkoutActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Читает workout-events — тот же топик, что и analytics-service, но своей consumer group
 * (group-id: notification-service), поэтому получает все сообщения независимо от аналитики.
 * Из "толстого" события тренировки берём только id, пользователя и дату; план на неделю храним копией.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutEventsListener {

    private final WorkoutActivityService activityService;
    private final WeeklyPlanSyncService planService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.workout-events}")
    public void onWorkoutEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        String eventType = EventHeaders.eventType(record);
        if (EventTypes.WORKOUT_COMPLETED.equals(eventType)) {
            activityService.completed(objectMapper.readValue(record.value(), WorkoutCompletedEvent.class));
        } else if (EventTypes.WORKOUT_DELETED.equals(eventType)) {
            activityService.deleted(objectMapper.readValue(record.value(), WorkoutDeletedEvent.class));
        } else if (EventTypes.WEEKLY_PLAN_UPDATED.equals(eventType)) {
            planService.apply(objectMapper.readValue(record.value(), WeeklyPlanUpdatedEvent.class));
        } else {
            log.debug("Workout event type {} is not handled by notifications", eventType);
        }
    }
}
