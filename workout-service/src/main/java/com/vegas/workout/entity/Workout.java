package com.vegas.workout.entity;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.NotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Тренировка — "агрегат": упражнения и подходы меняются ТОЛЬКО через её методы.
 * Так правила ("в завершённую нельзя добавлять подходы", "нумерация без дыр")
 * живут в одном месте, а не размазаны по сервисам.
 */
@Entity
@Table(name = "workouts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Двунаправленная связь: у WorkoutExercise есть поле workout (@ManyToOne), а здесь — mappedBy.
     * cascade = ALL: сохраняем/удаляем тренировку — то же происходит с её упражнениями.
     * orphanRemoval: убрали упражнение из списка -> Hibernate удалит строку из БД.
     * @OrderBy: порядок задаём сами полем position (см. renumber()).
     */
    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<WorkoutExercise> exercises = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Workout start(UUID userId, String name, UUID templateId, List<Exercise> exercises, Instant now) {
        Workout workout = new Workout();
        workout.userId = userId;
        workout.name = name;
        workout.templateId = templateId;
        workout.status = WorkoutStatus.IN_PROGRESS;
        workout.startedAt = now;
        exercises.forEach(workout::addExercise);
        return workout;
    }

    public WorkoutExercise addExercise(Exercise exercise) {
        ensureInProgress();
        boolean alreadyAdded = exercises.stream()
                .anyMatch(existing -> existing.getExercise().getId().equals(exercise.getId()));
        if (alreadyAdded) {
            throw new BadRequestException("Упражнение уже есть в тренировке");
        }
        WorkoutExercise workoutExercise = new WorkoutExercise(this, exercise, exercises.size());
        exercises.add(workoutExercise);
        return workoutExercise;
    }

    public void removeExercise(UUID workoutExerciseId) {
        ensureInProgress();
        exercises.remove(findExercise(workoutExerciseId));
        renumber();
    }

    public WorkoutExercise findExercise(UUID workoutExerciseId) {
        return exercises.stream()
                .filter(workoutExercise -> workoutExerciseId.equals(workoutExercise.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Упражнение в тренировке не найдено"));
    }

    public void complete(Instant now) {
        ensureInProgress();
        boolean hasAnySet = exercises.stream().anyMatch(workoutExercise -> !workoutExercise.getSets().isEmpty());
        if (!hasAnySet) {
            throw new BadRequestException("Нельзя завершить тренировку без подходов — удалите её, если она не нужна");
        }
        this.status = WorkoutStatus.COMPLETED;
        this.completedAt = now;
    }

    public void ensureInProgress() {
        if (!isInProgress()) {
            throw new BusinessException("Тренировка уже завершена");
        }
    }

    public boolean isInProgress() {
        return status == WorkoutStatus.IN_PROGRESS;
    }

    /** Рабочие подходы (без разминочных). */
    public long getWorkingSetsCount() {
        return exercises.stream()
                .flatMap(workoutExercise -> workoutExercise.getSets().stream())
                .filter(set -> !set.isWarmup())
                .count();
    }

    /** Тоннаж = сумма (вес × повторы) по рабочим подходам. */
    public BigDecimal getTonnageKg() {
        return exercises.stream()
                .flatMap(workoutExercise -> workoutExercise.getSets().stream())
                .map(WorkoutSet::getVolumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Длительность — только у завершённой; у идущей фронт сам считает таймер от startedAt. */
    public Long getDurationSec() {
        return completedAt == null ? null : Duration.between(startedAt, completedAt).toSeconds();
    }

    private void renumber() {
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).setPosition(i);
        }
    }
}
