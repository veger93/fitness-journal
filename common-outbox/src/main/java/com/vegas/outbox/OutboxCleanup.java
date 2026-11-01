package com.vegas.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Уборка outbox: отправленные события старше срока хранения удаляются раз в час.
 *
 * Отправленное событие уже лежит в Kafka — в таблице оно нужно только для разбора проблем ("ушло ли?").
 * Без уборки таблица растёт бесконечно; особенно заметно с демо (docs/demo.md): одно демо спортсмена —
 * ~75 событий о тренировках. Неотправленные (published_at IS NULL) не удаляются никогда.
 */
@Slf4j
public class OutboxCleanup {

    private final OutboxEventRepository repository;
    private final Clock clock;
    private final Duration retention;

    public OutboxCleanup(OutboxEventRepository repository, Clock clock, Duration retention) {
        this.repository = repository;
        this.clock = clock;
        this.retention = retention;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.outbox.cleanup-interval:PT1H}", initialDelayString = "${app.outbox.cleanup-initial-delay:PT5M}")
    public int deleteOldPublished() {
        int deleted = repository.deletePublishedBefore(clock.instant().minus(retention));
        if (deleted > 0) {
            log.info("Outbox cleanup: {} published events older than {} deleted", deleted, retention);
        }
        return deleted;
    }
}
