package com.vegas.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;

/**
 * День плана с тренировкой. @Embeddable — "значение" без своего id: живёт только внутри WeeklyPlan,
 * отдельно его не ищут и не сохраняют. Сравнение — по значению полей, поэтому record.
 */
@Embeddable
public record PlannedWorkout(
        @Enumerated(EnumType.STRING)
        @Column(name = "day_of_week", nullable = false, length = 9)
        DayOfWeek dayOfWeek,

        @Column(name = "template_name", nullable = false, length = 100)
        String templateName
) {
}
