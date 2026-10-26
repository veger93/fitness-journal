package com.vegas.workout.repository;

import com.vegas.workout.entity.WeeklyPlanDay;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WeeklyPlanDayRepository extends JpaRepository<WeeklyPlanDay, UUID> {

    /** План с комплексами и их упражнениями — одним запросом. */
    @EntityGraph(attributePaths = {"template", "template.exercises"})
    List<WeeklyPlanDay> findByUserId(UUID userId);

    /**
     * Удалить план пользователя ОДНИМ SQL-запросом и сразу.
     * Почему не derived deleteByUserId + saveAll: при сбросе (flush) Hibernate выполняет INSERT раньше DELETE,
     * и новая строка "понедельник" столкнулась бы со старой по UNIQUE (user_id, day_of_week).
     * Bulk-delete через @Query уходит в БД немедленно, до вставок.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from WeeklyPlanDay d where d.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
