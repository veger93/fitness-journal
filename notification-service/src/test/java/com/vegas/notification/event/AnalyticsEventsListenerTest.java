package com.vegas.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.MonthlyReportReadyEvent;
import com.vegas.common.event.PlateauDetectedEvent;
import com.vegas.notification.service.AnalyticsNotifier;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventsListenerTest {

    @Mock
    private AnalyticsNotifier notifier;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private AnalyticsEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new AnalyticsEventsListener(notifier, objectMapper);
    }

    @Test
    void plateau_isDispatched() throws Exception {
        PlateauDetectedEvent event = new PlateauDetectedEvent(UUID.randomUUID(), UUID.randomUUID(), "Присед",
                Instant.parse("2026-09-01T09:00:00Z"), 24);

        listener.onAnalyticsEvent(record(EventTypes.PLATEAU_DETECTED, objectMapper.writeValueAsString(event)));

        verify(notifier).plateau(event);
    }

    @Test
    void monthlyReport_isDispatched() throws Exception {
        MonthlyReportReadyEvent event = new MonthlyReportReadyEvent(UUID.randomUUID(), "2026-09",
                new BigDecimal("1.5"), null, null, 10, 2);

        listener.onAnalyticsEvent(record(EventTypes.MONTHLY_REPORT_READY, objectMapper.writeValueAsString(event)));

        verify(notifier).monthlyReport(event);
    }

    @Test
    void unknownType_isIgnored() throws Exception {
        listener.onAnalyticsEvent(record("SomethingNew", "{}"));
        listener.onAnalyticsEvent(record(null, "{}"));

        verifyNoInteractions(notifier);
    }

    private static ConsumerRecord<String, String> record(String eventType, String json) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("analytics-events", 0, 0L, "key", json);
        if (eventType != null) {
            record.headers().add(EventTypes.HEADER, eventType.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }
}
