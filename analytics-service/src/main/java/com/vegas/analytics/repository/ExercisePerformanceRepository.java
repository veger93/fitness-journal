package com.vegas.analytics.repository;

import com.vegas.analytics.entity.ExercisePerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExercisePerformanceRepository extends JpaRepository<ExercisePerformance, UUID> {

    /**
     * Массовое удаление одним SQL (DELETE ... WHERE workout_id = ?), выполняется СРАЗУ.
     *
     * Почему не derived-метод deleteByWorkoutId: он сначала загружает сущности, а удаляет
     * их только при flush. Hibernate при flush выполняет сначала INSERT, потом DELETE —
     * и вставка новых строк упёрлась бы в UNIQUE (workout_id, exercise_id) со старыми.
     */
    @Modifying
    @Query("delete from ExercisePerformance p where p.workoutId = :workoutId")
    int deleteByWorkoutId(@Param("workoutId") UUID workoutId);

    /** Точки графика прогресса одного упражнения за период, по возрастанию даты. */
    List<ExercisePerformance> findByUserIdAndExerciseIdAndPerformedAtGreaterThanEqualOrderByPerformedAtAsc(
            UUID userId, UUID exerciseId, Instant from);

    /** Вся история пользователя, по возрастанию даты — для ленты рекордов. */
    List<ExercisePerformance> findByUserIdOrderByPerformedAtAsc(UUID userId);
}
