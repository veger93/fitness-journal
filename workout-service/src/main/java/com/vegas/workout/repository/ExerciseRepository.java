package com.vegas.workout.repository;

import com.vegas.workout.entity.Exercise;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JpaSpecificationExecutor добавляет findAll(Specification...) — запросы с динамическими фильтрами
 * (есть bodyPart или нет, есть поиск или нет) без ручной склейки строк JPQL.
 */
public interface ExerciseRepository extends JpaRepository<Exercise, UUID>, JpaSpecificationExecutor<Exercise> {

    /**
     * Переопределяем метод только ради @EntityGraph: мышцы грузятся тем же запросом (LEFT JOIN).
     * Без этого — проблема N+1: 1 запрос за упражнениями + по запросу за мышцами КАЖДОГО из 30.
     */
    @Override
    @EntityGraph(attributePaths = "muscles")
    List<Exercise> findAll(Specification<Exercise> spec, Sort sort);

    /** Всё между find и By Spring Data игнорирует — "WithMuscles" просто для читаемости. */
    @EntityGraph(attributePaths = "muscles")
    Optional<Exercise> findWithMusclesById(UUID id);

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    /** Для редактирования: такое имя есть у ДРУГОГО упражнения этого пользователя? */
    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(UUID ownerId, String name, UUID id);
}
