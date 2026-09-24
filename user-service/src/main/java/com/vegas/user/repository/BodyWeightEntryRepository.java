package com.vegas.user.repository;

import com.vegas.user.entity.BodyWeightEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * "UserId" в имени метода Spring Data разбирает как путь user.id —
 * поэтому поле называется user, а искать можно по его id без JOIN-а.
 */
public interface BodyWeightEntryRepository extends JpaRepository<BodyWeightEntry, UUID> {

    /** История веса, новые сверху. */
    List<BodyWeightEntry> findAllByUserIdOrderByMeasuredOnDesc(UUID userId);

    /** Текущий вес = самая свежая запись. */
    Optional<BodyWeightEntry> findFirstByUserIdOrderByMeasuredOnDesc(UUID userId);

    /** Есть ли уже запись за этот день (тогда обновляем, а не создаём вторую). */
    Optional<BodyWeightEntry> findByUserIdAndMeasuredOn(UUID userId, LocalDate measuredOn);
}
