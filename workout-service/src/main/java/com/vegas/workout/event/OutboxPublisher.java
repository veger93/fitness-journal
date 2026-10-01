package com.vegas.workout.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Будильник: раз в poll-interval-ms запускает OutboxRelay.
 *
 * Почему отдельный класс, а не @Scheduled прямо в OutboxRelay рядом с @Transactional:
 * @Transactional работает через прокси Spring. Надёжнее, когда транзакционный метод
 * вызывается из ДРУГОГО бина — тогда вызов гарантированно идёт через прокси.
 */
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRelay outboxRelay;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        // если пачка полная — сразу берём следующую, не дожидаясь следующего тика
        while (outboxRelay.publishBatch() == OutboxRelay.BATCH_SIZE) {
            // продолжаем
        }
    }
}
