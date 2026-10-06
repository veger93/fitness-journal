package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Запись в ленте "Личные рекорды".
 * type — вид рекорда; valueKg — новое значение (1ПМ или вес), previousValueKg — прежний максимум.
 * weightKg × reps — подход, которым рекорд поставлен ("77 кг × 8").
 */
public record PersonalRecordResponse(
        RecordType type,
        UUID exerciseId,
        String exerciseName,
        Instant achievedAt,
        BigDecimal weightKg,
        Integer reps,
        BigDecimal valueKg,
        BigDecimal previousValueKg,
        BigDecimal deltaKg
) {
}
