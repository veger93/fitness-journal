package com.vegas.workout.repository;

import com.vegas.workout.entity.Program;
import com.vegas.workout.entity.ProgramSlot;
import com.vegas.workout.entity.ProgramStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    /** Комплексы-дни активной программы: их нельзя удалить или поменять в обход программы */
    @Query("""
            select distinct s.template.id from ProgramSlot s
            where s.program.ownerId = :ownerId and s.program.status = com.vegas.workout.entity.ProgramStatus.ACTIVE
            """)
    Set<UUID> findActiveTemplateIds(@Param("ownerId") UUID ownerId);

    /** Места активной программы в этом дне (комплексе) — цели подходов для тренировки по нему */
    @Query("""
            select s from ProgramSlot s join fetch s.exercise
            where s.program.ownerId = :ownerId and s.program.status = com.vegas.workout.entity.ProgramStatus.ACTIVE
              and s.template.id = :templateId
            """)
    List<ProgramSlot> findActiveSlots(@Param("ownerId") UUID ownerId, @Param("templateId") UUID templateId);
}
