package com.vegas.notification.entity;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyPlanTest {

    private static final Instant T1 = Instant.parse("2026-10-19T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-20T10:00:00Z");

    @Test
    void replace_newerEvent_replacesWholePlan() {
        WeeklyPlan plan = new WeeklyPlan(UUID.randomUUID());
        plan.replace(List.of(new PlannedWorkout(DayOfWeek.MONDAY, "Верх")), T1);

        boolean changed = plan.replace(List.of(new PlannedWorkout(DayOfWeek.FRIDAY, "Низ")), T2);

        assertThat(changed).isTrue();
        assertThat(plan.templateFor(DayOfWeek.MONDAY)).isEmpty();
        assertThat(plan.templateFor(DayOfWeek.FRIDAY)).contains("Низ");
    }

    @Test
    void replace_outdatedEvent_isIgnored() {
        WeeklyPlan plan = new WeeklyPlan(UUID.randomUUID());
        plan.replace(List.of(new PlannedWorkout(DayOfWeek.FRIDAY, "Низ")), T2);

        // старое событие пришло позже нового (повторная доставка) — план не откатываем
        boolean changed = plan.replace(List.of(new PlannedWorkout(DayOfWeek.MONDAY, "Верх")), T1);

        assertThat(changed).isFalse();
        assertThat(plan.templateFor(DayOfWeek.FRIDAY)).contains("Низ");
    }
}
