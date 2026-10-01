package com.vegas.workout.repository;

import com.vegas.workout.entity.WorkoutTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, UUID> {

    /**
     * Здесь фильтр всегда один и тот же, поэтому простой JPQL через @Query удобнее Specification.
     * JPQL пишется по сущностям и полям Java (WorkoutTemplate, ownerId), а не по таблицам.
     */
    @EntityGraph(attributePaths = "exercises")
    @Query("select t from WorkoutTemplate t where t.ownerId is null or t.ownerId = :userId order by t.name")
    List<WorkoutTemplate> findAllVisibleTo(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = "exercises")
    Optional<WorkoutTemplate> findWithExercisesById(UUID id);

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(UUID ownerId, String name, UUID id);
}
