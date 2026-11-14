package com.vegas.workout.program;

import java.time.DayOfWeek;
import java.util.List;

import static com.vegas.workout.program.Families.ABS;
import static com.vegas.workout.program.Families.BICEPS_CURL;
import static com.vegas.workout.program.Families.CALF_RAISE;
import static com.vegas.workout.program.Families.CHEST_FLY;
import static com.vegas.workout.program.Families.HINGE;
import static com.vegas.workout.program.Families.HORIZONTAL_PULL;
import static com.vegas.workout.program.Families.HORIZONTAL_PUSH;
import static com.vegas.workout.program.Families.LATERAL_RAISE;
import static com.vegas.workout.program.Families.LEG_CURL;
import static com.vegas.workout.program.Families.LUNGE;
import static com.vegas.workout.program.Families.REAR_DELT;
import static com.vegas.workout.program.Families.SQUAT;
import static com.vegas.workout.program.Families.TRICEPS_EXTENSION;
import static com.vegas.workout.program.Families.VERTICAL_PULL;
import static com.vegas.workout.program.Families.VERTICAL_PUSH;

/**
 * Сплит — как раскладываем тренировки по неделе (правило П1, docs/program.md):
 * 2–3 дня — всё тело, 4 — верх/низ, 5 — тяни/толкай/ноги + верх/низ. Каждая мышца — не реже 2 раз в неделю.
 *
 * Чертёж дня — семейства в порядке важности: сначала многосуставные (П7), потом изоляция.
 * Сколько мест взять — решает время тренировки (П9): короткая тренировка берёт первые 5, длинная — 7.
 * Поэтому в начале каждого чертежа — то, без чего день не имеет смысла.
 */
enum Split {

    FULL_BODY_2("Всё тело × 2",
            List.of(Blueprints.FULL_A, Blueprints.FULL_B),
            List.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            List.of(0, 1)),

    /** 3 дня: A · B · A. Неделя повторяется — день A чаще; при пересмотре можно поменять местами */
    FULL_BODY_3("Всё тело × 3",
            List.of(Blueprints.FULL_A, Blueprints.FULL_B),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            List.of(0, 1, 0)),

    UPPER_LOWER("Верх / низ",
            List.of(Blueprints.UPPER_A, Blueprints.LOWER_A, Blueprints.UPPER_B, Blueprints.LOWER_B),
            List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            List.of(0, 1, 2, 3)),

    PUSH_PULL_LEGS_UPPER_LOWER("Толкай / тяни / ноги + верх / низ",
            List.of(Blueprints.PUSH, Blueprints.PULL, Blueprints.LEGS, Blueprints.UPPER_A, Blueprints.LOWER_B),
            List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            List.of(0, 1, 2, 3, 4));

    final String title;
    /** Разные дни программы (комплексы) */
    final List<Blueprint> days;
    /** В какие дни недели тренировка (дни можно переставить в плане) */
    final List<DayOfWeek> weekDays;
    /** Какой день программы в какой день недели: индекс в days для каждого элемента weekDays */
    final List<Integer> schedule;

    Split(String title, List<Blueprint> days, List<DayOfWeek> weekDays, List<Integer> schedule) {
        this.title = title;
        this.days = days;
        this.weekDays = weekDays;
        this.schedule = schedule;
    }

    static Split forDays(int daysPerWeek) {
        return switch (daysPerWeek) {
            case 2 -> FULL_BODY_2;
            case 3 -> FULL_BODY_3;
            case 4 -> UPPER_LOWER;
            case 5 -> PUSH_PULL_LEGS_UPPER_LOWER;
            default -> throw new IllegalArgumentException("Дней в неделю: " + daysPerWeek); // ProgramAnswers не пропустит
        };
    }

    /** Место в чертеже: семейство и тип (многосуставное — тяжелее, больше отдыха, меньше повторов при цели «сила») */
    record Place(String family, boolean compound) {
        static Place compound(String family) {
            return new Place(family, true);
        }

        static Place isolation(String family) {
            return new Place(family, false);
        }
    }

    record Blueprint(String name, List<Place> places) {
    }

    /** Чертежи дней. Одно и то же семейство во второй день недели получит ДРУГОЕ упражнение (П8) */
    static final class Blueprints {
        static final Blueprint FULL_A = new Blueprint("День A · всё тело", List.of(
                Place.compound(SQUAT), Place.compound(HORIZONTAL_PUSH), Place.compound(VERTICAL_PULL),
                Place.compound(VERTICAL_PUSH), Place.isolation(LEG_CURL), Place.isolation(BICEPS_CURL),
                Place.isolation(TRICEPS_EXTENSION), Place.isolation(CALF_RAISE)));

        static final Blueprint FULL_B = new Blueprint("День B · всё тело", List.of(
                Place.compound(HINGE), Place.compound(HORIZONTAL_PUSH), Place.compound(HORIZONTAL_PULL),
                Place.compound(LUNGE), Place.isolation(LATERAL_RAISE), Place.isolation(TRICEPS_EXTENSION),
                Place.isolation(BICEPS_CURL), Place.isolation(ABS)));

        static final Blueprint UPPER_A = new Blueprint("Верх A", List.of(
                Place.compound(HORIZONTAL_PUSH), Place.compound(HORIZONTAL_PULL), Place.compound(VERTICAL_PUSH),
                Place.compound(VERTICAL_PULL), Place.isolation(LATERAL_RAISE), Place.isolation(BICEPS_CURL),
                Place.isolation(TRICEPS_EXTENSION)));

        static final Blueprint UPPER_B = new Blueprint("Верх B", List.of(
                Place.compound(VERTICAL_PULL), Place.compound(HORIZONTAL_PUSH), Place.compound(HORIZONTAL_PULL),
                Place.compound(VERTICAL_PUSH), Place.isolation(TRICEPS_EXTENSION), Place.isolation(BICEPS_CURL),
                Place.isolation(CHEST_FLY), Place.isolation(REAR_DELT)));

        static final Blueprint LOWER_A = new Blueprint("Низ A", List.of(
                Place.compound(SQUAT), Place.compound(HINGE), Place.compound(LUNGE),
                Place.isolation(LEG_CURL), Place.isolation(CALF_RAISE), Place.isolation(ABS)));

        static final Blueprint LOWER_B = new Blueprint("Низ B", List.of(
                Place.compound(HINGE), Place.compound(SQUAT), Place.compound(LUNGE),
                Place.isolation(LEG_CURL), Place.isolation(CALF_RAISE), Place.isolation(ABS)));

        static final Blueprint PUSH = new Blueprint("Толкай · грудь, плечи, трицепс", List.of(
                Place.compound(HORIZONTAL_PUSH), Place.compound(VERTICAL_PUSH), Place.compound(HORIZONTAL_PUSH),
                Place.isolation(LATERAL_RAISE), Place.isolation(TRICEPS_EXTENSION), Place.isolation(CHEST_FLY)));

        static final Blueprint PULL = new Blueprint("Тяни · спина, бицепс", List.of(
                Place.compound(VERTICAL_PULL), Place.compound(HORIZONTAL_PULL), Place.compound(VERTICAL_PULL),
                Place.isolation(REAR_DELT), Place.isolation(BICEPS_CURL), Place.isolation(BICEPS_CURL)));

        static final Blueprint LEGS = new Blueprint("Ноги", List.of(
                Place.compound(SQUAT), Place.compound(HINGE), Place.compound(LUNGE),
                Place.isolation(LEG_CURL), Place.isolation(CALF_RAISE), Place.isolation(ABS)));

        private Blueprints() {
        }
    }
}
