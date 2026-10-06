package com.vegas.notification.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.MonthlyReportReadyEvent;
import com.vegas.common.event.OverloadRiskEvent;
import com.vegas.common.event.PersonalRecordEvent;
import com.vegas.common.event.PlateauDetectedEvent;
import com.vegas.notification.service.AnalyticsNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Читает analytics-events и по заголовку eventType решает, какое письмо отправить. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventsListener {

    private final AnalyticsNotifier notifier;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.analytics-events}")
    public void onAnalyticsEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        String eventType = EventHeaders.eventType(record);
        if (eventType == null) {
            log.warn("Analytics event without {} header, offset {}", EventTypes.HEADER, record.offset());
            return;
        }
        switch (eventType) {
            case EventTypes.PERSONAL_RECORD -> notifier.personalRecord(read(record, PersonalRecordEvent.class));
            case EventTypes.PLATEAU_DETECTED -> notifier.plateau(read(record, PlateauDetectedEvent.class));
            case EventTypes.OVERLOAD_RISK -> notifier.overloadRisk(read(record, OverloadRiskEvent.class));
            case EventTypes.MONTHLY_REPORT_READY -> notifier.monthlyReport(read(record, MonthlyReportReadyEvent.class));
            default -> log.debug("Analytics event type {} is not handled", eventType);
        }
    }

    private <T> T read(ConsumerRecord<String, String> record, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(record.value(), type);
    }
}
