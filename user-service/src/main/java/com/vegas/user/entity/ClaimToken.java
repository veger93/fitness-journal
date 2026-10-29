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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Ссылка передачи аккаунта клиенту (таблица claim_tokens). Действует, пока не использована и не истекла */
@Entity
@Table(name = "claim_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "trainer_id", nullable = false, updatable = false)
    private UUID trainerId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ClaimToken(UUID clientId, UUID trainerId, String tokenHash, Instant expiresAt) {
        this.clientId = clientId;
        this.trainerId = trainerId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isValid(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }
}
