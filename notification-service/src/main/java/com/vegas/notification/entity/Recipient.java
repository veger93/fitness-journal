package com.vegas.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Получатель писем: email из user-service (событие UserRegistered). */
@Entity
@Table(name = "recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipient {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Recipient(UUID userId, String email, String displayName) {
        this.userId = userId;
        update(email, displayName);
    }

    public void update(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    /** Как обращаться в письме: по имени, а если его нет — нейтрально. */
    public String greetingName() {
        return displayName == null || displayName.isBlank() ? "Привет" : displayName;
    }
}
