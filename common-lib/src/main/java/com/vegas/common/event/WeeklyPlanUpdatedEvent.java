package com.vegas.common.event;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * План на неделю изменился (сохранили план, переименовали или удалили комплекс из плана).
 * Событие несёт план ЦЕЛИКОМ, а не "что поменялось": получателю не нужно ничего склеивать,
 * он просто заменяет у себя копию. Пустой days — плана больше нет.
 *
 * updatedAt — чтобы получатель отбросил устаревшее событие, если оно придёт позже нового.
 */
public record WeeklyPlanUpdatedEvent(
        UUID userId,
        List<PlannedDay> days,
        Instant updatedAt
) {
    /** День с тренировкой. Дни отдыха в план не попадают. */
    public record PlannedDay(DayOfWeek dayOfWeek, String templateName) {
    }
}
