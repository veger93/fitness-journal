package com.vegas.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Новый личный рекорд (одна карточка: упражнение в тренировке, внутри — виды рекорда).
 * type — WEIGHT ("Вес") или E1RM ("Сила"); hint — пояснение простыми словами.
 */
public record PersonalRecordEvent(
        UUID userId,
        UUID workoutId,
        UUID exerciseId,
        String exerciseName,
        Instant achievedAt,
        BigDecimal weightKg,
        Integer reps,
        List<Item> records
) {
    public record Item(String type, BigDecimal valueKg, BigDecimal deltaKg, String hint) {
    }
}
