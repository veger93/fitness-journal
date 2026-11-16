package com.vegas.workout.dto;

import com.vegas.workout.entity.Equipment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Вариант для окна «Заменить»: то же семейство, подходит по месту и ограничениям.
 * Уже делал — в окне строка «уже делали: 32 кг × 10, 3 нед. назад» (числа форматирует фронт).
 *
 * @param lastWeightKg лучший рабочий подход последней тренировки с этим упражнением; null — не делал или без веса
 * @param usedInDay    упражнение уже стоит в другом дне программы («Верх B») — заменить можно, но будет повтор;
 *                     null — в программе его нет. В этом же дне повтора быть не может: такие в варианты не попадают
 */
public record ProgramAlternativeResponse(
        UUID exerciseId,
        String name,
        Equipment equipment,
        String imageKey,
        boolean familiar,
        BigDecimal lastWeightKg,
        Integer lastReps,
        Instant lastPerformedAt,
        String usedInDay
) {
}
