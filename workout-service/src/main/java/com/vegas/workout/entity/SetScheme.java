package com.vegas.workout.entity;

import com.vegas.common.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Схема упражнения в программе: «3 × 8–12, отдых 2 мин».
 * @Embeddable — значение внутри ProgramSlot, своих таблицы и id нет: колонки лежат прямо в program_slots.
 * Неизменяемое: поменять схему = положить новую (так проще не забыть проверки).
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SetScheme {

    @Column(name = "sets", nullable = false)
    private short sets;

    @Column(name = "reps_min", nullable = false)
    private short repsMin;

    @Column(name = "reps_max", nullable = false)
    private short repsMax;

    @Column(name = "rest_sec", nullable = false)
    private short restSec;

    private SetScheme(int sets, int repsMin, int repsMax, int restSec) {
        this.sets = (short) sets;
        this.repsMin = (short) repsMin;
        this.repsMax = (short) repsMax;
        this.restSec = (short) restSec;
    }

    /** Те же границы, что CHECK-и в миграции 022: ошибку в генераторе увидим здесь, а не на INSERT */
    public static SetScheme of(int sets, int repsMin, int repsMax, int restSec) {
        if (sets < 1 || sets > 10) {
            throw new BadRequestException("Подходов — от 1 до 10");
        }
        if (repsMin < 1 || repsMin > repsMax || repsMax > 50) {
            throw new BadRequestException("Повторы — от 1 до 50, нижняя граница не больше верхней");
        }
        if (restSec < 30 || restSec > 600) {
            throw new BadRequestException("Отдых — от 30 секунд до 10 минут");
        }
        return new SetScheme(sets, repsMin, repsMax, restSec);
    }
}
