package com.vegas.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Факт тренировки (см. миграцию 004). Нужна только дата — подробности живут в workout-service. */
@Entity
@Table(name = "workout_activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutActivity {

    @Id
    @Column(name = "workout_id")
    private UUID workoutId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    public WorkoutActivity(UUID workoutId, UUID userId, Instant completedAt) {
        this.workoutId = workoutId;
        this.userId = userId;
        this.completedAt = completedAt;
    }
}
