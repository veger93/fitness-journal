package com.vegas.workout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Разгрузка по упражнению (см. миграцию 011). Пока она активна, подсказка подхода предлагает
 * облегчённый вес и схему вместо обычной прогрессии.
 * Ссылка на упражнение — просто id: сущность Exercise здесь не нужна, а лишний JOIN — да.
 */
@Entity
@Table(name = "deload_periods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeloadPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "working_weight_kg", precision = 6, scale = 2)
    private BigDecimal workingWeightKg;

    @Column(nullable = false)
    private int sets;

    @Column(nullable = false)
    private int reps;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    public DeloadPeriod(UUID userId, UUID exerciseId, BigDecimal workingWeightKg, int sets, int reps,
                        Instant startedAt, Instant endsAt) {
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.workingWeightKg = workingWeightKg;
        this.sets = sets;
        this.reps = reps;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    public boolean isActiveAt(Instant moment) {
        return endsAt.isAfter(moment);
    }

    /** Закончить раньше срока: строку оставляем — это история, просто срок теперь "сейчас". */
    public void endAt(Instant moment) {
        if (isActiveAt(moment)) {
            this.endsAt = moment;
        }
    }
}
