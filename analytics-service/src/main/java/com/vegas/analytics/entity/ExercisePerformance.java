package com.vegas.analytics.entity;

import com.vegas.common.model.BodyPart;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Итог упражнения в тренировке (см. миграцию 001).
 * @Builder: полей много, и почти все обязательные — конструктор с 15 параметрами нечитаем,
 * а builder показывает имя каждого поля при создании.
 */
@Entity
@Table(name = "exercise_performances")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExercisePerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "workout_id", nullable = false)
    private UUID workoutId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_part", nullable = false, length = 20)
    private BodyPart bodyPart;

    @Column(name = "tracking_type", nullable = false, length = 20)
    private String trackingType;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "working_sets", nullable = false)
    private int workingSets;

    @Column(name = "total_reps")
    private Integer totalReps;

    @Column(name = "volume_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumeKg;

    @Column(name = "best_weight_kg", precision = 6, scale = 2)
    private BigDecimal bestWeightKg;

    @Column(name = "best_reps")
    private Integer bestReps;

    @Column(name = "best_e1rm_kg", precision = 7, scale = 2)
    private BigDecimal bestE1rmKg;

    /** Самый тяжёлый рабочий подход (при равном весе — с большим числом повторов). */
    @Column(name = "max_weight_kg", precision = 6, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "max_weight_reps")
    private Integer maxWeightReps;

    /** Средний RPE рабочих подходов; null — пользователь RPE не указывал. */
    @Column(name = "avg_rpe", precision = 3, scale = 1)
    private BigDecimal avgRpe;

    @Column(name = "best_duration_sec")
    private Integer bestDurationSec;

    @Column(name = "best_distance_m")
    private Integer bestDistanceM;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
