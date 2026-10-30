package com.vegas.user.entity;

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

/** Код из письма (таблица email_codes). Действует, пока не использован, не истёк и не исчерпаны попытки */
@Entity
@Table(name = "email_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailCode {

    public enum Purpose {
        VERIFY_EMAIL,
        RESET_PASSWORD
    }

    /** 6 цифр перебираются за миллион попыток — поэтому не больше 5 неверных на код */
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Purpose purpose;

    @Column(nullable = false, updatable = false)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64, updatable = false)
    private String codeHash;

    @Column(nullable = false)
    private short attempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    /** Время ставим сами (из Clock), а не @CreationTimestamp: по нему считаем "повторно через 60 сек", и это тестируется */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EmailCode(UUID userId, Purpose purpose, String email, String codeHash, Instant createdAt, Instant expiresAt) {
        this.userId = userId;
        this.purpose = purpose;
        this.email = email;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isActive(Instant now) {
        return usedAt == null && attempts < MAX_ATTEMPTS && now.isBefore(expiresAt);
    }

    public void failedAttempt() {
        attempts++;
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }
}
