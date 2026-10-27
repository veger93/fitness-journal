package com.vegas.workout.repository;

import com.vegas.workout.entity.DeloadPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeloadPeriodRepository extends JpaRepository<DeloadPeriod, UUID> {

    /** Активные разгрузки пользователя: срок ещё не вышел. "Сейчас" передаём параметром — так метод легко тестировать. */
    List<DeloadPeriod> findByUserIdAndEndsAtAfter(UUID userId, Instant now);

    List<DeloadPeriod> findByUserIdAndExerciseIdAndEndsAtAfter(UUID userId, UUID exerciseId, Instant now);
}
