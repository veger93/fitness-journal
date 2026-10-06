package com.vegas.analytics.dto;

import java.math.BigDecimal;

/**
 * Один вид рекорда внутри карточки — на фронте это бейдж ("Вес +2.5 кг", "Сила +2.9 кг").
 * weightKg × reps — подход, которым рекорд поставлен (у разных видов может быть разным).
 * hint — короткое пояснение простыми словами, показывается мелким шрифтом как сноска:
 * "тот же вес, но на 2 повтора больше".
 */
public record RecordItemResponse(
        RecordType type,
        BigDecimal weightKg,
        Integer reps,
        BigDecimal valueKg,
        BigDecimal previousValueKg,
        BigDecimal deltaKg,
        String hint
) {
}
