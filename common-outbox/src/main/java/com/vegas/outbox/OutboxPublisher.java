package com.vegas.outbox;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Будильник: раз в app.outbox.poll-interval-ms запускает OutboxRelay.
 * Отдельный бин, чтобы транзакционный publishBatch() вызывался снаружи — через прокси Spring.
 */
public class OutboxPublisher {

    private final OutboxRelay outboxRelay;

    public OutboxPublisher(OutboxRelay outboxRelay) {
        this.outboxRelay = outboxRelay;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        // пачка полная — сразу берём следующую, не дожидаясь тика
        while (outboxRelay.publishBatch() == outboxRelay.batchSize()) {
            // продолжаем
        }
    }
}
