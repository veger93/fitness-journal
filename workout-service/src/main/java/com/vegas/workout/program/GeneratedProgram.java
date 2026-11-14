package com.vegas.workout.program;

import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.SetScheme;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * Что собрал генератор — ещё не сущности и не в базе. Сервис (этап 3) превратит это в комплексы,
 * программу с местами и план недели. Так генератор остаётся чистой функцией: легко тестировать, нечего мокать.
 *
 * @param title    «Всё тело × 3»
 * @param schedule день недели → индекс в days
 * @param notes    честные пояснения человеку: что заменили и почему («Дома нет тяги сверху — …»)
 */
public record GeneratedProgram(String title, List<Day> days, Map<DayOfWeek, Integer> schedule, List<String> notes) {

    public record Day(String name, List<Slot> slots) {
    }

    public record Slot(Exercise exercise, String movementPattern, SetScheme scheme, boolean compound) {
    }
}
