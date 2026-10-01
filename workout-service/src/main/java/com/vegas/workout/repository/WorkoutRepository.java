package com.vegas.workout.repository;

import com.vegas.workout.dto.WorkoutSummaryResponse;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    /**
     * Тренировка + упражнения + данные упражнений из каталога одним запросом.
     * Подходы догрузятся отдельным запросом благодаря @BatchSize в WorkoutExercise.
     * userId в условии — тренировку можно получить только свою.
     */
    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Workout> findWithExercisesByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Workout> findWithExercisesByUserIdAndStatus(UUID userId, WorkoutStatus status);

    boolean existsByUserIdAndStatus(UUID userId, WorkoutStatus status);

    /**
     * История с пагинацией. DTO собирается прямо в запросе (select new ...):
     * в память не грузятся ни упражнения, ни подходы — только итоговые цифры.
     * "left join ... on s.warmup = false" — присоединяем только рабочие подходы.
     * countQuery нужен Spring Data, чтобы посчитать общее число строк для totalPages.
     */
    @Query(value = """
            select new com.vegas.workout.dto.WorkoutSummaryResponse(
                w.id, w.name, w.startedAt, w.completedAt,
                count(distinct we.id), count(s.id), sum(s.weightKg * s.reps))
            from Workout w
            left join w.exercises we
            left join we.sets s on s.warmup = false
            where w.userId = :userId and w.status = :status
            group by w.id, w.name, w.startedAt, w.completedAt
            order by w.startedAt desc
            """,
            countQuery = "select count(w) from Workout w where w.userId = :userId and w.status = :status")
    Page<WorkoutSummaryResponse> findHistory(@Param("userId") UUID userId,
                                             @Param("status") WorkoutStatus status,
                                             Pageable pageable);
}
