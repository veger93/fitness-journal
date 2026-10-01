package com.vegas.workout.repository;

import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, UUID> {

    /**
     * Это упражнение в последних завершённых тренировках пользователя (новые первыми).
     * Limit — сколько строк вернуть (LIMIT в SQL); нам нужна одна, самая свежая.
     */
    @Query("""
            select we from WorkoutExercise we
            join we.workout w
            where w.userId = :userId and w.status = :status and we.exercise.id = :exerciseId
            order by w.completedAt desc
            """)
    List<WorkoutExercise> findRecent(@Param("userId") UUID userId,
                                     @Param("exerciseId") UUID exerciseId,
                                     @Param("status") WorkoutStatus status,
                                     Limit limit);
}
