package com.vegas.outbox;

import com.vegas.common.event.EventTypes;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Переносит события из outbox-таблицы в Kafka.
 *
 * Гарантия — "at least once" (как минимум один раз): если отправка прошла, а пометка
 * "отправлено" не успела закоммититься, событие уйдёт повторно. Поэтому получатель
 * обязан быть идемпотентным: повторная обработка не должна ничего портить.
 */
@Slf4j
public class OutboxRelay {

    private static final long SEND_TIMEOUT_SEC = 10;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;
    private final String topic;
    private final int batchSize;

    public OutboxRelay(OutboxEventRepository outboxRepository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       Clock clock,
                       OutboxProperties properties) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.topic = properties.topic();
        this.batchSize = properties.batchSize();
    }

    public int batchSize() {
        return batchSize;
    }

    /** @return сколько событий отправлено */
    @Transactional
    public int publishBatch() {
        List<OutboxEvent> events = outboxRepository.lockNextBatch(batchSize);
        int published = 0;

        for (OutboxEvent event : events) {
            try {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(topic, event.getEventKey(), event.getPayload());
                record.headers().add(EventTypes.HEADER, event.getEventType().getBytes(StandardCharsets.UTF_8));

                // send() асинхронный и возвращает Future; get() ждёт подтверждения от брокера
                kafkaTemplate.send(record).get(SEND_TIMEOUT_SEC, TimeUnit.SECONDS);

                event.markPublished(clock.instant());
                published++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event.markFailed(e);
                break;
            } catch (Exception e) {
                log.warn("Outbox event {} ({}) not published: {}", event.getId(), event.getEventType(), e.getMessage());
                event.markFailed(e);
                // Останавливаемся на первой ошибке: если отправить следующие, нарушится порядок событий
                break;
            }
        }
        return published;
    }
}
