package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Главная экран "с наполнением" одним запросом.
 *
 * empty = true — тренировок ещё нет (на фронте empty state "Начать тренировку").
 * weekTonnageKg — сколько поднято за последние 7 дней (сумма "вес × повторы"), previousWeekTonnageKg — за 7 дней до этого;
 * weekTonnageChangePercent = null, если на прошлой неделе тренировок не было.
 * focusExerciseId — упражнение для большого графика: первое, требующее внимания, иначе последнее выполненное с весом.
 * exercises — отсортированы: сначала проблемы (перегрузка, плато), затем рекорды, затем по дате.
 */
public record DashboardResponse(
        boolean empty,
        String month,
        int workoutsThisMonth,
        BigDecimal weekTonnageKg,
        BigDecimal previousWeekTonnageKg,
        BigDecimal weekTonnageChangePercent,
        UUID focusExerciseId,
        List<ExerciseSummaryResponse> exercises
) {
}
