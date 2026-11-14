package com.vegas.workout.dto;

import com.vegas.workout.entity.BodyLimit;
import com.vegas.workout.entity.ExperienceLevel;
import com.vegas.workout.entity.PriorityGroup;
import com.vegas.workout.entity.ProgramAnswers;
import com.vegas.workout.entity.ProgramGoal;
import com.vegas.workout.entity.ProgramLocation;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Ответы анкеты (docs/program.md). Ответов о здоровье здесь нет специально: их фронт не отправляет —
 * при «да» он только советует сходить к врачу.
 * Границы (дни 2–5, минуты 45/60/75) проверяет ProgramAnswers — одно место для правил.
 *
 * {"goal":"MUSCLE","daysPerWeek":3,"sessionMinutes":60,"location":"GYM","experienceLevel":"BEGINNER",
 *  "limits":["LOWER_BACK"],"priority":null}
 */
public record ProgramRequest(
        @NotNull(message = "goal обязателен") ProgramGoal goal,
        @NotNull(message = "daysPerWeek обязателен") Integer daysPerWeek,
        @NotNull(message = "sessionMinutes обязателен") Integer sessionMinutes,
        @NotNull(message = "location обязателен") ProgramLocation location,
        @NotNull(message = "experienceLevel обязателен") ExperienceLevel experienceLevel,
        Set<BodyLimit> limits,
        PriorityGroup priority
) {
    public ProgramAnswers toAnswers() {
        return new ProgramAnswers(goal, daysPerWeek, sessionMinutes, location, experienceLevel, limits, priority);
    }
}
