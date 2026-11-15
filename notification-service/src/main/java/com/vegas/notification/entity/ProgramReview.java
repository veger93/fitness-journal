package com.vegas.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Дата пересмотра программы пользователя — копия из workout-service (миграция 008) */
@Entity
@Table(name = "program_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramReview {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "review_on", nullable = false)
    private LocalDate reviewOn;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProgramReview(UUID userId, UUID programId, LocalDate reviewOn, Instant updatedAt) {
        this.userId = userId;
        this.programId = programId;
        this.reviewOn = reviewOn;
        this.updatedAt = updatedAt;
    }

    /** Новая программа. false — событие старее того, что уже сохранено (Kafka могла доставить его позже) */
    public boolean replace(UUID programId, LocalDate reviewOn, Instant changedAt) {
        if (changedAt.isBefore(updatedAt)) {
            return false;
        }
        this.programId = programId;
        this.reviewOn = reviewOn;
        this.updatedAt = changedAt;
        return true;
    }
}
