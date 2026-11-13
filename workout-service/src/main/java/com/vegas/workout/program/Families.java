package com.vegas.workout.program;

import com.vegas.workout.entity.PriorityGroup;

import java.util.Map;
import java.util.Optional;

/**
 * Семейства движения (exercises.movement_pattern, миграция 019, docs/analytics.md) — словарь генератора.
 * В сущности Exercise семейство — строка (оно нужно и аналитике), поэтому здесь константы и подписи,
 * а не enum: новое семейство в каталоге не ломает генератор, он его просто не использует.
 */
final class Families {

    static final String SQUAT = "SQUAT";
    static final String HINGE = "HINGE";
    static final String LUNGE = "LUNGE";
    static final String HORIZONTAL_PUSH = "HORIZONTAL_PUSH";
    static final String VERTICAL_PUSH = "VERTICAL_PUSH";
    static final String HORIZONTAL_PULL = "HORIZONTAL_PULL";
    static final String VERTICAL_PULL = "VERTICAL_PULL";
    static final String CHEST_FLY = "CHEST_FLY";
    static final String LATERAL_RAISE = "LATERAL_RAISE";
    static final String REAR_DELT = "REAR_DELT";
    static final String BICEPS_CURL = "BICEPS_CURL";
    static final String TRICEPS_EXTENSION = "TRICEPS_EXTENSION";
    static final String LEG_CURL = "LEG_CURL";
    static final String CALF_RAISE = "CALF_RAISE";
    static final String ABS = "ABS";

    /** Подписи для пояснений человеку: «Дома нет упражнений на «тягу сверху» — поставили «тягу к поясу»» */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry(SQUAT, "присед"),
            Map.entry(HINGE, "наклон (становая, румынская тяга)"),
            Map.entry(LUNGE, "выпады"),
            Map.entry(HORIZONTAL_PUSH, "жим лёжа"),
            Map.entry(VERTICAL_PUSH, "жим вверх"),
            Map.entry(HORIZONTAL_PULL, "тяга к поясу"),
            Map.entry(VERTICAL_PULL, "тяга сверху"),
            Map.entry(CHEST_FLY, "разведения на грудь"),
            Map.entry(LATERAL_RAISE, "махи в стороны"),
            Map.entry(REAR_DELT, "задняя дельта"),
            Map.entry(BICEPS_CURL, "бицепс"),
            Map.entry(TRICEPS_EXTENSION, "трицепс"),
            Map.entry(LEG_CURL, "сгибание ног"),
            Map.entry(CALF_RAISE, "икры"),
            Map.entry(ABS, "пресс"));

    /**
     * Нет упражнений семейства (дома нет тренажёров, ограничения убрали все варианты) —
     * чем заменить место так, чтобы работали примерно те же мышцы. Нет в карте — место пропускаем.
     */
    private static final Map<String, String> FALLBACK = Map.of(
            VERTICAL_PULL, HORIZONTAL_PULL,
            LEG_CURL, HINGE,
            CHEST_FLY, HORIZONTAL_PUSH,
            REAR_DELT, HORIZONTAL_PULL,
            LUNGE, SQUAT);

    /** Какую группу «подтянуть в первую очередь» (анкета) прокачивает семейство */
    private static final Map<String, PriorityGroup> GROUP = Map.ofEntries(
            Map.entry(HORIZONTAL_PUSH, PriorityGroup.CHEST),
            Map.entry(CHEST_FLY, PriorityGroup.CHEST),
            Map.entry(HORIZONTAL_PULL, PriorityGroup.BACK),
            Map.entry(VERTICAL_PULL, PriorityGroup.BACK),
            Map.entry(SQUAT, PriorityGroup.LEGS),
            Map.entry(HINGE, PriorityGroup.LEGS),
            Map.entry(LUNGE, PriorityGroup.LEGS),
            Map.entry(LEG_CURL, PriorityGroup.LEGS),
            Map.entry(CALF_RAISE, PriorityGroup.LEGS),
            Map.entry(BICEPS_CURL, PriorityGroup.ARMS),
            Map.entry(TRICEPS_EXTENSION, PriorityGroup.ARMS),
            Map.entry(VERTICAL_PUSH, PriorityGroup.SHOULDERS),
            Map.entry(LATERAL_RAISE, PriorityGroup.SHOULDERS),
            Map.entry(REAR_DELT, PriorityGroup.SHOULDERS));

    private Families() {
    }

    static String label(String family) {
        return LABELS.getOrDefault(family, family);
    }

    static Optional<String> fallback(String family) {
        return Optional.ofNullable(FALLBACK.get(family));
    }

    static Optional<PriorityGroup> group(String family) {
        return Optional.ofNullable(GROUP.get(family));
    }
}
