package com.vegas.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Событие, ожидающее отправки в Kafka (см. миграцию 008 и OutboxRelay). */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    public static OutboxEvent create(UUID aggregateId, String eventType, String eventKey, String payload, Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.eventKey = eventKey;
        event.payload = payload;
        event.createdAt = now;
        return event;
    }

    public void markPublished(Instant now) {
        this.publishedAt = now;
        this.lastError = null;
    }

    public void markFailed(Exception error) {
        this.attempts++;
        String message = String.valueOf(error.getMessage());
        this.lastError = message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
