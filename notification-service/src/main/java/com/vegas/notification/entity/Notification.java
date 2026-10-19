package com.vegas.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Запись журнала уведомлений (см. миграцию 003). */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "dedupe_key", nullable = false, length = 200)
    private String dedupeKey;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Notification sent(UUID userId, NotificationType type, String dedupeKey, EmailMessage message, Instant now) {
        return create(userId, type, dedupeKey, message, NotificationStatus.SENT, null, now);
    }

    public static Notification skipped(UUID userId, NotificationType type, String dedupeKey, EmailMessage message,
                                       String reason, Instant now) {
        return create(userId, type, dedupeKey, message, NotificationStatus.SKIPPED, reason, now);
    }

    private static Notification create(UUID userId, NotificationType type, String dedupeKey, EmailMessage message,
                                       NotificationStatus status, String reason, Instant now) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.type = type;
        notification.dedupeKey = dedupeKey;
        notification.subject = message.subject();
        notification.body = message.body();
        notification.status = status;
        notification.reason = reason;
        notification.createdAt = now;
        return notification;
    }
}
