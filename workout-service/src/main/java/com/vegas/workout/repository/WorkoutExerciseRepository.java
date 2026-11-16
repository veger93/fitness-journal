package com.vegas.workout.repository;

import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, UUID> {

    /**
     * Это упражнение в последних завершённых тренировках пользователя (новые первыми).
     * Только где были подходы: упражнение могли оставить в списке и не сделать — такой «прошлый раз» пустой,
     * и подсказки (прошлый раз, прибавка веса) считали бы его «новым».
     * Limit — сколько строк вернуть (LIMIT в SQL); нам нужна одна, самая свежая.
     */
    @Query("""
            select we from WorkoutExercise we
            join we.workout w
            where w.userId = :userId and w.status = :status and we.exercise.id = :exerciseId
              and we.sets is not empty
            order by w.completedAt desc
            """)
    List<WorkoutExercise> findRecent(@Param("userId") UUID userId,
                                     @Param("exerciseId") UUID exerciseId,
                                     @Param("status") WorkoutStatus status,
                                     Limit limit);

    /** Есть ли это упражнение хоть в одной тренировке (любого статуса). exercise.id — вложенное свойство. */
    boolean existsByExerciseId(UUID exerciseId);

    /** Какие упражнения человек уже делал (в завершённых тренировках) — генератор и окно замены ставят их первыми */
    @Query("""
            select distinct we.exercise.id from WorkoutExercise we
            join we.workout w
            where w.userId = :userId and w.status = :status and we.sets is not empty
            """)
    Set<UUID> findDoneExerciseIds(@Param("userId") UUID userId, @Param("status") WorkoutStatus status);
}
