package com.vegas.notification.repository;

import com.vegas.notification.entity.WorkoutActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutActivityRepository extends JpaRepository<WorkoutActivity, UUID> {

    /** max() по пустой выборке вернёт NULL — Spring Data превратит его в Optional.empty(). */
    @Query("select max(a.completedAt) from WorkoutActivity a where a.userId = :userId")
    Optional<Instant> findLastCompletedAt(@Param("userId") UUID userId);
}
