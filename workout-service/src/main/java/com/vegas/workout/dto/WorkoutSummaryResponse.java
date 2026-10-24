package com.vegas.workout.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Строка в "Истории тренировок". Собирается прямо в SQL-запросе (см. WorkoutRepository.findHistory),
 * без загрузки упражнений и подходов в память.
 * Типы — обёртки (Long), т.к. count() в JPQL возвращает Long, а sum() по пустому набору — null.
 */
public record WorkoutSummaryResponse(
        UUID id,
        String name,
        Instant startedAt,
        Instant completedAt,
        Long exerciseCount,
        Long workingSets,
        BigDecimal tonnageKg,
        Long distanceM          // сумма дистанций рабочих подходов (бег); 0, если не бегали
) {
    /** Компактный конструктор record: выполняется перед присвоением полей — нормализуем null. */
    public WorkoutSummaryResponse {
        if (tonnageKg == null) {
            tonnageKg = BigDecimal.ZERO;
        }
        if (distanceM == null) {
            distanceM = 0L;
        }
    }
}
