package com.vegas.workout.entity;

import com.vegas.common.exception.NotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Упражнение внутри конкретной тренировки + его подходы. */
@Entity
@Table(name = "workout_exercises")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(nullable = false)
    private int position;

    /**
     * @BatchSize: когда понадобятся подходы, Hibernate загрузит их сразу для 50 упражнений
     * одним запросом (WHERE workout_exercise_id IN (...)), а не по запросу на каждое.
     * Почему не через @EntityGraph, как упражнения: две List-коллекции одним JOIN-ом
     * Hibernate грузить отказывается (MultipleBagFetchException) — получилось бы декартово произведение.
     */
    @OneToMany(mappedBy = "workoutExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    private List<WorkoutSet> sets = new ArrayList<>();

    WorkoutExercise(Workout workout, Exercise exercise, int position) {
        this.workout = workout;
        this.exercise = exercise;
        this.position = position;
    }

    public WorkoutSet addSet(SetValues values, Instant now) {
        workout.ensureInProgress();
        WorkoutSet set = new WorkoutSet(this, sets.size(), values, now);
        sets.add(set);
        return set;
    }

    public void removeSet(UUID setId) {
        workout.ensureInProgress();
        sets.remove(findSet(setId));
        for (int i = 0; i < sets.size(); i++) {
            sets.get(i).setPosition(i);
        }
    }

    public WorkoutSet findSet(UUID setId) {
        return sets.stream()
                .filter(set -> setId.equals(set.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Подход не найден"));
    }

    void setPosition(int position) {
        this.position = position;
    }
}
