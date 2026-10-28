package com.vegas.workout.repository;

import com.vegas.workout.entity.ExerciseTechnique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Ключ — id упражнения, поэтому findById(exerciseId) — и есть "техника упражнения". */
public interface ExerciseTechniqueRepository extends JpaRepository<ExerciseTechnique, UUID> {
}
