package com.vegas.workout.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Цель упражнения в тренировке по дню программы: сколько подходов, повторов и отдыха (место программы) +
 * подсказка веса по двойной прогрессии (П10, program/Progression). У свободной тренировки — null.
 *
 * @param suggestion        INCREASE — прибавить (два раза подряд верхняя граница), KEEP — прежний вес, NEW — подобрать
 * @param suggestedWeightKg что подставить в подход; null — подбирает человек
 * @param baseWeightKg      рабочий вес прошлого раза — для пояснения «на 57,5 кг было 12, 12, 12»
 * @param lastReps          повторы на рабочем весе в прошлый раз
 */
public record ExerciseTargetResponse(
        int sets,
        int repsMin,
        int repsMax,
        int restSec,
        String suggestion,
        BigDecimal suggestedWeightKg,
        BigDecimal baseWeightKg,
        List<Integer> lastReps
) {
}
