package com.vegas.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Настройки из application.yml:
 *   app.outbox.topic: workout-events   — куда отправлять события этого сервиса
 *   app.outbox.batch-size: 100          — сколько событий за один проход
 *   app.outbox.poll-interval-ms: 1000   — читается прямо в @Scheduled (см. OutboxPublisher)
 *   app.outbox.retention: P7D           — сколько хранить отправленные события (OutboxCleanup)
 */
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(
        String topic,
        @DefaultValue("100") int batchSize,
        @DefaultValue("1000") long pollIntervalMs,
        @DefaultValue("P7D") Duration retention
) {
}
