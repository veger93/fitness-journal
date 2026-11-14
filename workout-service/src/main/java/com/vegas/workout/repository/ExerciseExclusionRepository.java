package com.vegas.workout.repository;

import com.vegas.workout.entity.ExerciseExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface ExerciseExclusionRepository extends JpaRepository<ExerciseExclusion, ExerciseExclusion.Key> {

    /** Только id упражнений — генератору и окну замены больше и не нужно */
    @Query("select e.exerciseId from ExerciseExclusion e where e.ownerId = :ownerId")
    Set<UUID> findExerciseIdsByOwnerId(@Param("ownerId") UUID ownerId);
}
