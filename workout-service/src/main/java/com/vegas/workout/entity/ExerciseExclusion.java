package com.vegas.workout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * «Больше не предлагать»: генератор программы и окно замены не берут это упражнение.
 *
 * Составной ключ (owner_id, exercise_id): у строки нет своего смысла без пары «кто + что», поэтому отдельный
 * UUID не нужен. В JPA составной ключ — @IdClass: два поля с @Id + класс Key с теми же полями.
 */
@Entity
@Table(name = "exercise_exclusions")
@IdClass(ExerciseExclusion.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseExclusion {

    @Id
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Id
    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ExerciseExclusion(UUID ownerId, UUID exerciseId) {
        this.ownerId = ownerId;
        this.exerciseId = exerciseId;
    }

    /** Ключ: поля и их имена — те же, что у @Id-полей сущности. equals/hashCode обязательны (сравнение по значению) */
    @Getter
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Key implements Serializable {
        private UUID ownerId;
        private UUID exerciseId;
    }
}
