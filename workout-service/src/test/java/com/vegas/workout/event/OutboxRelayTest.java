package com.vegas.workout.event;

import com.vegas.common.event.EventTypes;
import com.vegas.workout.entity.OutboxEvent;
import com.vegas.workout.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outboxRepository, kafkaTemplate, Clock.fixed(NOW, ZoneOffset.UTC), "workout-events");
    }

    @Test
    void publishesWithKeyAndTypeHeader_andMarksAsPublished() {
        OutboxEvent event = event("user-1");
        when(outboxRepository.lockNextBatch(OutboxRelay.BATCH_SIZE)).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(ok());

        int published = relay.publishBatch();

        assertThat(published).isEqualTo(1);
        assertThat(event.getPublishedAt()).isEqualTo(NOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(sent.capture());
        assertThat(sent.getValue().topic()).isEqualTo("workout-events");
        assertThat(sent.getValue().key()).isEqualTo("user-1");
        assertThat(new String(sent.getValue().headers().lastHeader(EventTypes.HEADER).value(), StandardCharsets.UTF_8))
                .isEqualTo(EventTypes.WORKOUT_COMPLETED);
    }

    @Test
    void stopsOnFirstFailure_toKeepEventOrder() {
        OutboxEvent first = event("user-1");
        OutboxEvent second = event("user-1");
        OutboxEvent third = event("user-1");
        when(outboxRepository.lockNextBatch(OutboxRelay.BATCH_SIZE)).thenReturn(List.of(first, second, third));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(ok())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker is down")));

        int published = relay.publishBatch();

        assertThat(published).isEqualTo(1);
        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNull();
        assertThat(second.getAttempts()).isEqualTo(1);
        assertThat(second.getLastError()).contains("broker is down");
        assertThat(third.getAttempts()).isZero();          // до третьего дело не дошло
        verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
    }

    private static OutboxEvent event(String key) {
        return OutboxEvent.create(UUID.randomUUID(), EventTypes.WORKOUT_COMPLETED, key, "{}", NOW);
    }

    private static CompletableFuture<SendResult<String, String>> ok() {
        return CompletableFuture.completedFuture(null);
    }
}
