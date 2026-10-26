package com.vegas.workout.dto;

import java.time.DayOfWeek;
import java.util.List;

/** План на неделю: всегда 7 дней с понедельника; template = null — день отдыха. */
public record WeeklyPlanResponse(List<Day> days) {

    public record Day(DayOfWeek dayOfWeek, WorkoutTemplateResponse template) {
    }
}
