package com.vegas.analytics.dto;

import java.util.List;

/**
 * Отчёт за месяц. Один ответ для двух экранов:
 * модалка "Итоги месяца" берёт strength / tonnage / workouts, страница отчёта — ещё и exercises.
 *
 * empty = true — в этом месяце тренировок не было (на фронте empty state "Отчёт ещё формируется").
 * strength = null, если ни в одном упражнении нет расчётного 1ПМ (только упражнения со своим весом).
 */
public record MonthlyReportResponse(
        String month,
        boolean empty,
        MetricChangeResponse strength,
        MetricChangeResponse tonnage,
        WorkoutsCountResponse workouts,
        int recordsCount,
        List<ExerciseMonthResponse> exercises
) {
}
