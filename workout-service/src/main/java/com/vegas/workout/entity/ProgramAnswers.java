package com.vegas.workout.entity;

import com.vegas.common.exception.BadRequestException;

import java.util.Set;

/**
 * Ответы анкеты — то, по чему генератор собирает программу (docs/program.md, раздел «Анкета»).
 * Не сущность, а значение: record. Хранятся в колонках programs (и program_limits для «что беспокоит»).
 *
 * Проверки — в конструкторе (compact constructor record'а): неверные ответы не могут даже появиться в коде,
 * а не проверяются «где-то потом». Те же правила стоят CHECK-ами в миграции 022 — двойная страховка.
 *
 * @param priority null — «всё ровно»
 */
public record ProgramAnswers(
        ProgramGoal goal,
        int daysPerWeek,
        int sessionMinutes,
        ProgramLocation location,
        ExperienceLevel experienceLevel,
        Set<BodyLimit> limits,
        PriorityGroup priority
) {
    public static final int MIN_DAYS = 2;
    public static final int MAX_DAYS = 5;
    /** Варианты из анкеты: ~45, ~60, 75+ минут */
    public static final Set<Integer> SESSION_MINUTES = Set.of(45, 60, 75);

    public ProgramAnswers {
        if (goal == null || location == null || experienceLevel == null) {
            throw new BadRequestException("Выберите цель, место тренировок и уровень подготовки");
        }
        if (daysPerWeek < MIN_DAYS || daysPerWeek > MAX_DAYS) {
            throw new BadRequestException("Дней в неделю — от " + MIN_DAYS + " до " + MAX_DAYS);
        }
        if (!SESSION_MINUTES.contains(sessionMinutes)) {
            throw new BadRequestException("Время тренировки — 45, 60 или 75 минут");
        }
        // копия: снаружи набор могут поменять, а ответы должны остаться такими, какими пришли
        limits = limits == null ? Set.of() : Set.copyOf(limits);
    }
}
