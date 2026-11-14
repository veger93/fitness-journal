package com.vegas.workout.repository;

import com.vegas.workout.entity.Program;
import com.vegas.workout.entity.ProgramStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    /**
     * Активная программа человека — с ответами («что беспокоит») и местами одним запросом (@EntityGraph),
     * без N+1: иначе Hibernate догружал бы limits и каждое место отдельными SELECT-ами.
     */
    @EntityGraph(attributePaths = {"limits", "slots", "slots.exercise"})
    Optional<Program> findByOwnerIdAndStatus(UUID ownerId, ProgramStatus status);

    default Optional<Program> findActive(UUID ownerId) {
        return findByOwnerIdAndStatus(ownerId, ProgramStatus.ACTIVE);
    }
}
