package com.vegas.notification.entity;

public enum NotificationStatus {
    SENT,     // письмо отправлено
    SKIPPED,  // не отправлено (например, нет email) — но записано, чтобы не пытаться снова
    IN_APP    // только в ленте приложения, письмо не отправляем (уведомления тренеру о клиентах)
}
