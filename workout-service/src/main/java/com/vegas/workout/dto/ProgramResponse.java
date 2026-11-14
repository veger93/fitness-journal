package com.vegas.workout.dto;

import com.vegas.workout.entity.BodyLimit;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.ExperienceLevel;
import com.vegas.workout.entity.PriorityGroup;
import com.vegas.workout.entity.ProgramGoal;
import com.vegas.workout.entity.ProgramLocation;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Программа для экранов «Ваша программа» и «День программы».
 *
 * @param title      «Всё тело × 3»
 * @param reviewDue  прошло 6 недель — фронт предложит пройти анкету заново
 * @param notes      пояснения генератора: что заменили и почему
 */
public record ProgramResponse(
        UUID id,
        String title,
        ProgramGoal goal,
        int daysPerWeek,
        int sessionMinutes,
        ProgramLocation location,
        ExperienceLevel experienceLevel,
        Set<BodyLimit> limits,
        PriorityGroup priority,
        Instant startedAt,
        LocalDate reviewAt,
        boolean reviewDue,
        List<String> notes,
        List<Day> days
) {
    /**
     * День программы = комплекс. weekDays — по текущему плану недели: человек мог переставить дни сам.
     */
    public record Day(UUID templateId, String name, List<DayOfWeek> weekDays, List<Slot> slots) {
    }

    /**
     * @param swapped             менять уже нельзя (П14: один раз)
     * @param originalExerciseName что стояло до замены
     */
    public record Slot(
            UUID id,
            int position,
            UUID exerciseId,
            String exerciseName,
            Equipment equipment,
            String imageKey,
            String movementPattern,
            int sets,
            int repsMin,
            int repsMax,
            int restSec,
            boolean swapped,
            String originalExerciseName
    ) {
    }
}
