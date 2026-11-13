package com.vegas.workout.program;

import com.vegas.workout.entity.BodyLimit;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.ExperienceLevel;
import com.vegas.workout.entity.ProgramAnswers;
import com.vegas.workout.entity.ProgramGoal;
import com.vegas.workout.entity.ProgramLocation;
import com.vegas.workout.entity.SetScheme;
import com.vegas.workout.entity.TrackingType;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.vegas.workout.program.Families.BICEPS_CURL;
import static com.vegas.workout.program.Families.HINGE;
import static com.vegas.workout.program.Families.HORIZONTAL_PULL;
import static com.vegas.workout.program.Families.HORIZONTAL_PUSH;
import static com.vegas.workout.program.Families.LUNGE;
import static com.vegas.workout.program.Families.SQUAT;
import static com.vegas.workout.program.Families.VERTICAL_PULL;
import static com.vegas.workout.program.Families.VERTICAL_PUSH;

/**
 * Генератор программы: ответы анкеты + каталог → программа (docs/program.md, правила П1–П14).
 *
 * Чистый класс: без Spring, без базы, без времени. Одни и те же входы — всегда одна и та же программа
 * (сортировки детерминированы), поэтому каждое правило проверяется обычным юнит-тестом (ProgramGeneratorTest).
 * Сервис (этап 3) достаёт из базы каталог, исключения и «что уже делал» и передаёт сюда.
 */
public class ProgramGenerator {

    /** П9: время тренировки → сколько упражнений (≈ 3 подхода × (подход + 2 мин отдыха) ≈ 9–10 мин на упражнение) */
    static final Map<Integer, Integer> PLACES_BY_MINUTES = Map.of(45, 5, 60, 6, 75, 7);

    /** Приоритетной группе — +1 подход (анкета «что подтянуть»), но не больше этого */
    static final int MAX_SETS = 5;

    /**
     * Классика семейства — мягкое предпочтение при равном снаряде: при прочих равных ставим упражнение,
     * которое знают все и которое проще повторять неделями. Переименовали в каталоге — просто перестанет влиять.
     */
    private static final Map<String, List<String>> CLASSIC = Map.of(
            HORIZONTAL_PUSH, List.of("Жим лёжа", "Жим гантелей лёжа", "Жим от груди в тренажёре",
                    "Жим лёжа на наклонной скамье", "Жим гантелей на наклонной скамье"),
            VERTICAL_PULL, List.of("Тяга верхнего блока", "Подтягивания", "Вертикальная тяга в рычажном тренажёре"),
            HORIZONTAL_PULL, List.of("Тяга штанги в наклоне", "Тяга горизонтального блока", "Тяга гантели одной рукой"),
            VERTICAL_PUSH, List.of("Жим гантелей сидя", "Жим штанги стоя"),
            SQUAT, List.of("Приседания со штангой", "Жим ногами", "Приседания с гантелями"),
            HINGE, List.of("Румынская тяга", "Становая тяга", "Становая тяга с гантелями", "Гиперэкстензия"),
            LUNGE, List.of("Выпады с гантелями", "Обратные выпады с гантелями"),
            BICEPS_CURL, List.of("Подъём штанги на бицепс", "Подъём гантелей на бицепс"));

    /**
     * Свой вес, но нужен турник, брусья или станок — дома с гантелями их нет.
     * Признака «нужно оборудование» в каталоге пока нет, узнаём по названию; добавить признак — задача
     * в «Нет в каталоге» (docs/catalog.md).
     */
    private static final List<String> NEEDS_GYM_EQUIPMENT = List.of("подтягиван", "брусь", "в висе", "гиперэкстенз");

    public GeneratedProgram generate(ProgramAnswers answers, List<Exercise> catalog,
                                     Set<UUID> excluded, Set<UUID> familiar) {
        Split split = Split.forDays(answers.daysPerWeek());
        int placesPerDay = PLACES_BY_MINUTES.get(answers.sessionMinutes());
        List<Exercise> pool = catalog.stream().filter(e -> usable(e, answers, excluded)).toList();
        Set<String> notes = new LinkedHashSet<>(); // одна и та же замена в двух днях — одно пояснение
        Set<UUID> usedInProgram = new HashSet<>();

        List<GeneratedProgram.Day> days = new ArrayList<>();
        for (Split.Blueprint blueprint : split.days) {
            Set<UUID> usedInDay = new HashSet<>();
            List<GeneratedProgram.Slot> slots = new ArrayList<>();
            for (Split.Place place : blueprint.places()) {
                if (slots.size() == placesPerDay) {
                    break; // П9: короткая тренировка берёт только первые места чертежа (они важнее)
                }
                Optional<Exercise> exercise = pickWithFallback(place.family(), pool, answers, usedInDay,
                        usedInProgram, familiar, notes);
                exercise.ifPresent(e -> {
                    slots.add(new GeneratedProgram.Slot(e, e.getMovementPattern(), scheme(place, e, answers),
                            place.compound()));
                    usedInDay.add(e.getId());
                    usedInProgram.add(e.getId());
                });
            }
            days.add(new GeneratedProgram.Day(blueprint.name(), slots));
        }

        Map<DayOfWeek, Integer> schedule = new EnumMap<>(DayOfWeek.class);
        for (int i = 0; i < split.weekDays.size(); i++) {
            schedule.put(split.weekDays.get(i), split.schedule.get(i));
        }
        if (answers.goal() == ProgramGoal.FAT_LOSS) {
            notes.add("Для похудения добавьте кардио: 150–300 минут умеренной нагрузки в неделю (быстрая ходьба, "
                    + "велосипед). Силовые сохраняют мышцы, пока уходит жир.");
        }
        return new GeneratedProgram(split.title, days, schedule, List.copyOf(notes));
    }

    // ---------- П8: выбор упражнения ----------

    private Optional<Exercise> pickWithFallback(String family, List<Exercise> pool, ProgramAnswers answers,
                                                Set<UUID> usedInDay, Set<UUID> usedInProgram, Set<UUID> familiar,
                                                Set<String> notes) {
        Optional<Exercise> exercise = pick(family, pool, answers, usedInDay, usedInProgram, familiar);
        if (exercise.isPresent()) {
            return exercise;
        }
        Optional<String> fallback = Families.fallback(family);
        Optional<Exercise> replacement = fallback
                .flatMap(f -> pick(f, pool, answers, usedInDay, usedInProgram, familiar));
        if (replacement.isPresent()) {
            notes.add("Нет подходящих упражнений на «" + Families.label(family) + "» — поставили «"
                    + Families.label(fallback.get()) + "»" + reason(answers) + ".");
        } else {
            notes.add("Нет подходящих упражнений на «" + Families.label(family) + "» — это место пропустили"
                    + reason(answers) + ".");
        }
        return replacement;
    }

    private static String reason(ProgramAnswers answers) {
        if (answers.location() == ProgramLocation.HOME_DUMBBELLS) {
            return " (дома только гантели и свой вес)";
        }
        return answers.limits().isEmpty() ? "" : " (учли, что беспокоит)";
    }

    /**
     * Порядок выбора:
     * 1) упражнение, которого ещё нет в программе — во второй день недели то же семейство, но другое упражнение
     *    (П8: систематическая вариация полезна, Kassiano 2022);
     * 2) то, что человек уже делал, — есть история и понятен стартовый вес;
     * 3) снаряд: новичку в зале — сначала тренажёр (проще техника), остальным — свободные веса;
     * 4) классика семейства; 5) название — чтобы выбор был всегда одинаковым.
     */
    private Optional<Exercise> pick(String family, List<Exercise> pool, ProgramAnswers answers,
                                    Set<UUID> usedInDay, Set<UUID> usedInProgram, Set<UUID> familiar) {
        Comparator<Exercise> order = Comparator
                .comparing((Exercise e) -> usedInProgram.contains(e.getId()))
                .thenComparing(e -> !familiar.contains(e.getId()))
                .thenComparingInt(e -> equipmentRank(e.getEquipment(), answers))
                .thenComparingInt(e -> classicRank(family, e.getName()))
                .thenComparing(Exercise::getName);
        return pool.stream()
                .filter(e -> family.equals(e.getMovementPattern()))
                .filter(e -> !usedInDay.contains(e.getId())) // в одном дне упражнение — один раз
                .min(order);
    }

    private static int equipmentRank(Equipment equipment, ProgramAnswers answers) {
        List<Equipment> order;
        if (answers.location() == ProgramLocation.HOME_DUMBBELLS) {
            order = List.of(Equipment.DUMBBELL, Equipment.BODYWEIGHT);
        } else if (answers.experienceLevel() == ExperienceLevel.BEGINNER) {
            order = List.of(Equipment.MACHINE, Equipment.DUMBBELL, Equipment.BARBELL, Equipment.BODYWEIGHT);
        } else {
            order = List.of(Equipment.BARBELL, Equipment.DUMBBELL, Equipment.MACHINE, Equipment.BODYWEIGHT);
        }
        int rank = order.indexOf(equipment);
        return rank < 0 ? order.size() : rank;
    }

    private static int classicRank(String family, String name) {
        int rank = CLASSIC.getOrDefault(family, List.of()).indexOf(name);
        return rank < 0 ? Integer.MAX_VALUE : rank;
    }

    // ---------- какие упражнения вообще годятся ----------

    private static boolean usable(Exercise e, ProgramAnswers answers, Set<UUID> excluded) {
        return !e.isCustom()                                  // свои упражнения — без семейства (пока)
                && !e.isArchived()
                && e.getMovementPattern() != null             // без семейства не заменить (П14)
                && e.getTrackingType() == TrackingType.WEIGHT_REPS // планка и бег — не про подходы × повторы
                && !e.isAssisted()                            // гравитрон — помощь, а не базовый выбор
                && !excluded.contains(e.getId())              // «больше не предлагать»
                && allowedEquipment(e, answers.location())
                && !blockedByLimits(e, answers.limits());
    }

    private static boolean allowedEquipment(Exercise e, ProgramLocation location) {
        if (location == ProgramLocation.GYM) {
            return true;
        }
        if (e.getEquipment() == Equipment.DUMBBELL) {
            return true;
        }
        String name = e.getName().toLowerCase();
        return e.getEquipment() == Equipment.BODYWEIGHT && NEEDS_GYM_EQUIPMENT.stream().noneMatch(name::contains);
    }

    /**
     * П13 — «что беспокоит». Консервативная логика, не медицинский совет: убираем упражнения
     * с осевой нагрузкой и неудобными положениями. Источник подберём отдельно (docs/program.md, открытый вопрос).
     */
    static boolean blockedByLimits(Exercise e, Set<BodyLimit> limits) {
        String family = e.getMovementPattern();
        boolean barbell = e.getEquipment() == Equipment.BARBELL;
        String name = e.getName().toLowerCase();
        for (BodyLimit limit : limits) {
            boolean blocked = switch (limit) {
                case KNEES -> LUNGE.equals(family) || (SQUAT.equals(family) && barbell);
                case LOWER_BACK -> barbell && (HINGE.equals(family) || SQUAT.equals(family)
                        || HORIZONTAL_PULL.equals(family) || VERTICAL_PUSH.equals(family));
                // флага «брусья» в каталоге нет — узнаём по названию (каталог правится миграциями, docs/catalog.md)
                case SHOULDERS -> (VERTICAL_PUSH.equals(family) && barbell)
                        || name.contains("брусьях") || name.contains("к подбородку");
            };
            if (blocked) {
                return true;
            }
        }
        return false;
    }

    // ---------- П2–П6: схема подходов ----------

    /**
     * Подходы (П2, П3): от уровня — новичок 3 в базовых / 2 в изоляции, средний 3/3, опытный 4/3;
     * приоритетной группе +1. Повторы (П4): мышцы 8–12 (изоляция 10–15), сила 4–6 в базовых, похудение 10–15.
     * Отдых (П6): базовые 2 мин (сила — 3), изоляция 1,5 мин.
     */
    static SetScheme scheme(Split.Place place, Exercise exercise, ProgramAnswers answers) {
        int sets = switch (answers.experienceLevel()) {
            case BEGINNER -> place.compound() ? 3 : 2;
            case INTERMEDIATE -> 3;
            case ADVANCED -> place.compound() ? 4 : 3;
        };
        boolean priority = answers.priority() != null
                && Families.group(exercise.getMovementPattern()).filter(answers.priority()::equals).isPresent();
        if (priority) {
            sets = Math.min(sets + 1, MAX_SETS);
        }
        return switch (answers.goal()) {
            case MUSCLE -> place.compound()
                    ? SetScheme.of(sets, 8, 12, 120)
                    : SetScheme.of(sets, 10, 15, 90);
            case STRENGTH -> place.compound()
                    ? SetScheme.of(sets, 4, 6, 180)
                    : SetScheme.of(sets, 8, 12, 90);
            case FAT_LOSS -> SetScheme.of(sets, 10, 15, place.compound() ? 120 : 90);
        };
    }
}
