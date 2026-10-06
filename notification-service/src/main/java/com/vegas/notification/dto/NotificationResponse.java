package com.vegas.notification.dto;

import com.vegas.notification.entity.Notification;
import com.vegas.notification.entity.NotificationStatus;
import com.vegas.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

/** Строка истории уведомлений (для экрана "Уведомления"). dedupeKey — внутренняя деталь, наружу не отдаём. */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String subject,
        String body,
        NotificationStatus status,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getSubject(), n.getBody(), n.getStatus(), n.getCreatedAt());
    }
}
