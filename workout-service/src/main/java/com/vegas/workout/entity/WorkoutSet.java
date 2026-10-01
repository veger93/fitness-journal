package com.vegas.workout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Один подход. */
@Entity
@Table(name = "workout_sets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;

    @Column(nullable = false)
    private int position;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    private Integer reps;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(precision = 3, scale = 1)
    private BigDecimal rpe;

    @Column(nullable = false)
    private boolean warmup;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    WorkoutSet(WorkoutExercise workoutExercise, int position, SetValues values, Instant now) {
        this.workoutExercise = workoutExercise;
        this.position = position;
        this.completedAt = now;
        apply(values);
    }

    public void update(SetValues values) {
        workoutExercise.getWorkout().ensureInProgress();
        apply(values);
    }

    /** Объём подхода (вес × повторы). Разминочные и подходы без веса в тоннаж не идут. */
    public BigDecimal getVolumeKg() {
        if (warmup || weightKg == null || reps == null) {
            return BigDecimal.ZERO;
        }
        return weightKg.multiply(BigDecimal.valueOf(reps));
    }

    void setPosition(int position) {
        this.position = position;
    }

    private void apply(SetValues values) {
        this.weightKg = values.weightKg();
        this.reps = values.reps();
        this.durationSec = values.durationSec();
        this.distanceM = values.distanceM();
        this.rpe = values.rpe();
        this.warmup = values.warmup();
    }
}
