package com.vegas.workout.demo;

import com.vegas.workout.entity.SetValues;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Сценарии демо (docs/demo.md): какие тренировки, когда и с какими подходами. Чистые функции — только
 * "сейчас" на входе, поэтому всё проверяется тестами без базы (DemoScenariosTest).
 *
 * Даты — "N дней назад" от сейчас, поэтому демо всегда свежее: "рекорд вчера" — действительно вчера.
 *
 * Важно для сигналов аналитики (InsightsCalculator):
 * - плато = 21+ день без нового максимума 1ПМ при 3+ тренировках, а "свежий рекорд" на главной — 14 дней.
 *   Вспомогательные упражнения растут раз в 3 недели так, чтобы последний рост был 15–20 дней назад:
 *   ни плато, ни рекорда — спокойная карточка. Иначе главная пестрела бы "рекордами" или "плато";
 * - перегрузка (HIGH) = объём за неделю > 1,5× среднего за месяц (+2) и 1ПМ на 5%+ ниже недавнего пика (+2),
 *   плюс RPE 9+ (+1) — так устроена последняя неделя "перегруженного" упражнения.
 */
public final class DemoScenarios {

    /** Один подход — каждые 3 минуты; конец тренировки — через 5 минут после последнего подхода */
    static final Duration SET_EVERY = Duration.ofMinutes(3);
    /**
     * Тренировка "N дней назад" начинается за полтора часа до этого же момента N дней назад: тогда
     * "вчера" — это действительно 1+ полных суток (аналитика считает дни как полные сутки, Duration.toDays),
     * и "рекорд · вчера" не превратится в "рекорд · сегодня", когда гость зашёл утром.
     */
    private static final Duration BEFORE_NOW = Duration.ofMinutes(90);
    private static final BigDecimal RPE_NORMAL = new BigDecimal("7.5");
    private static final BigDecimal RPE_HARD = new BigDecimal("9.5");

    public static final String ATHLETE_PUSH = "Грудь и спина";
    public static final String ATHLETE_LEGS = "Ноги";
    public static final String ATHLETE_ARMS = "Плечи и руки";
    public static final String COACH_LEGS = "Ноги и ягодицы";
    public static final String COACH_UPPER = "Верх тела";

    private DemoScenarios() {
    }

    /** Одна тренировка демо */
    public record Session(Instant start, String name, String template, List<Exercise> exercises, boolean inProgress) {
        public Instant end() {
            int sets = exercises.stream().mapToInt(e -> e.sets().size()).sum();
            return start.plus(SET_EVERY.multipliedBy(sets)).plus(Duration.ofMinutes(5));
        }
    }

    /** Упражнение по названию из каталога и его подходы */
    public record Exercise(String name, List<SetValues> sets) {
    }

    /** Неизвестный сценарий — пустой список: генератор ничего не создаст (и не упадёт) */
    public static List<Session> plan(String scenario, Instant now) {
        List<Session> sessions = switch (scenario) {
            case "ATHLETE" -> athlete(now);
            case "COACH" -> coach(now);
            case "CLIENT_IN_PROGRESS" -> clientInProgress(now);
            case "CLIENT_OVERLOAD" -> clientOverload(now);
            case "CLIENT_PLATEAU" -> clientPlateau(now);
            case "CLIENT_AWAY" -> clientAway(now);
            case "CLIENT_RECORD" -> clientRecord(now);
            default -> List.of();
        };
        List<Session> sorted = new ArrayList<>(sessions);
        // по порядку: события WorkoutCompleted уйдут от старых к новым, аналитика увидит историю как в жизни
        sorted.sort(Comparator.comparing(Session::start));
        return sorted;
    }

    // ---------- спортсмен «Алексей»: 18 недель, Пн/Ср/Пт + пробежка ----------

    static List<Session> athlete(Instant now) {
        int weeks = 18;
        List<Session> sessions = new ArrayList<>();
        for (int w = 0; w < weeks; w++) {
            int ago = 7 * (weeks - 1 - w); // w = 0 — самая старая неделя, последняя — текущая
            // грудь и спина: жим растёт до свежего рекорда — последняя тренировка вчера
            sessions.add(session(now, ago + 1, ATHLETE_PUSH, ATHLETE_PUSH,
                    main("Жим лёжа", grow(70, 2.5, (w + 1) / 2, 2.5), 5, 4),
                    bodyweight("Подтягивания", 3, 7 + w / 4),
                    work("Тяга штанги в наклоне", grow(50, 2.5, w / 3, 2.5), 8, 3)));
            // ноги: присед рос до ~31 дня назад, потом 4 тренировки без нового максимума — плато
            // (w + 1) / 2: вес растёт на нечётных неделях — последний рост на неделе 13 (31 день назад);
            // дальше тот же вес, повторов столько же или меньше: 1ПМ не выше максимума
            boolean squatPlateau = ago + 3 < 31;
            double squat = squatPlateau ? grow(85, 2.5, 7, 2.5) : grow(85, 2.5, (w + 1) / 2, 2.5);
            int squatReps = squatPlateau && w % 2 == 0 ? 4 : 5;
            sessions.add(session(now, ago + 3, ATHLETE_LEGS, ATHLETE_LEGS,
                    main("Приседания со штангой", squat, squatReps, 4),
                    work("Румынская тяга", grow(60, 2.5, w / 3, 2.5), 8, 3),
                    work("Выпады с гантелями", grow(14, 1, w / 3, 1), 10, 3)));
            // плечи и руки + становая: последняя неделя — перегрузка (см. ниже, отдельная тренировка)
            boolean lastWeek = w == weeks - 1;
            double deadlift = lastWeek ? 132.5 : grow(110, 2.5, w / 2, 2.5);
            sessions.add(session(now, ago + 5, ATHLETE_ARMS, ATHLETE_ARMS,
                    lastWeek ? hard("Становая тяга", deadlift, 5, 5) : main("Становая тяга", deadlift, 5, 3),
                    work("Жим штанги стоя", grow(40, 2.5, w / 3, 2.5), 6, 3),
                    work("Подъём штанги на бицепс", grow(30, 2.5, w / 3, 2.5), 10, 3),
                    timed("Планка", 3, 60 + w * 4)));
            // пробежка раз в неделю: +150 м каждую неделю (без резкого роста — иначе "нагрузка выросла")
            sessions.add(session(now, ago + 6, "Пробежка", null,
                    run(5000 + w * 150, 330)));
        }
        // перегрузка становой: доп. тренировка 2 дня назад — объём ×2, вес ниже, почти до отказа
        sessions.add(session(now, 2, "Становая — добивка", null, hard("Становая тяга", 120, 5, 5)));
        return sessions;
    }

    // ---------- тренер «Марина»: свои тренировки, 8 недель, 2 раза в неделю ----------

    static List<Session> coach(Instant now) {
        int weeks = 8;
        List<Session> sessions = new ArrayList<>();
        for (int w = 0; w < weeks; w++) {
            int ago = 7 * (weeks - 1 - w);
            sessions.add(session(now, ago + 2, COACH_LEGS, COACH_LEGS,
                    main("Приседания со штангой", grow(50, 2.5, (w + 1) / 3, 2.5), 6, 4),
                    work("Румынская тяга", grow(50, 2.5, (w + 1) / 3, 2.5), 8, 3),
                    work("Выпады с гантелями", grow(10, 1, (w + 1) / 3, 1), 10, 3)));
            sessions.add(session(now, ago + 5, COACH_UPPER, COACH_UPPER,
                    work("Жим гантелей лёжа", grow(16, 1, (w + 1) / 3, 1), 8, 3),
                    work("Тяга верхнего блока", grow(40, 2.5, (w + 1) / 3, 2.5), 10, 3),
                    work("Жим гантелей сидя", grow(10, 1, (w + 1) / 3, 1), 10, 3)));
        }
        return sessions;
    }

    // ---------- клиенты демо-тренера: 10 недель, 2 раза в неделю ----------

    /** Базовая программа клиента: всё растёт раз в 2 недели. offset — с какого "дня назад" начинается неделя */
    private static List<Session> clientBase(Instant now, int weeks, int offset, double bench, double squat) {
        List<Session> sessions = new ArrayList<>();
        for (int w = 0; w < weeks; w++) {
            int ago = 7 * (weeks - 1 - w) + offset;
            sessions.add(session(now, ago + 3, "Тренировка с тренером", null,
                    main("Жим лёжа", grow(bench, 2.5, w / 2, 2.5), 8, 3),
                    work("Тяга верхнего блока", grow(30, 2.5, w / 2, 2.5), 10, 3)));
            sessions.add(session(now, ago, "Тренировка с тренером", null,
                    main("Приседания со штангой", grow(squat, 2.5, w / 2, 2.5), 8, 3),
                    work("Жим ногами", grow(80, 5, w / 2, 5), 10, 3)));
        }
        return sessions;
    }

    /** Иван: обычный прогресс + тренировка идёт прямо сейчас (начата 25 минут назад) */
    static List<Session> clientInProgress(Instant now) {
        List<Session> sessions = clientBase(now, 10, 2, 40, 50);
        Instant started = now.minus(Duration.ofMinutes(25));
        sessions.add(new Session(started, "Тренировка с тренером", null, List.of(
                new Exercise("Жим лёжа", sets(52.5, 8, 3, RPE_NORMAL)),
                new Exercise("Тяга верхнего блока", List.of())), true));
        return sessions;
    }

    /** Ольга: на последней неделе присед — объём ×2 и вес упал: риск перегрузки */
    static List<Session> clientOverload(Instant now) {
        List<Session> sessions = new ArrayList<>();
        int weeks = 10;
        for (int w = 0; w < weeks; w++) {
            int ago = 7 * (weeks - 1 - w);
            boolean lastWeek = w == weeks - 1;
            sessions.add(session(now, ago + 4, "Тренировка с тренером", null,
                    work("Жим гантелей лёжа", grow(8, 1, w / 2, 1), 10, 3),
                    work("Тяга верхнего блока", grow(25, 2.5, w / 2, 2.5), 10, 3)));
            sessions.add(session(now, ago + 6, "Тренировка с тренером", null,
                    lastWeek ? hard("Приседания со штангой", 52.5, 5, 5)
                            : main("Приседания со штангой", grow(40, 2.5, w / 2, 2.5), 5, 3),
                    work("Румынская тяга", grow(30, 2.5, w / 2, 2.5), 10, 3)));
        }
        sessions.add(session(now, 1, "Тренировка с тренером", null, hard("Приседания со штангой", 47.5, 5, 5)));
        return sessions;
    }

    /** Пётр: жим не растёт ~4 недели (тот же вес, повторов меньше) — плато; остальное растёт */
    static List<Session> clientPlateau(Instant now) {
        List<Session> sessions = new ArrayList<>();
        int weeks = 10;
        for (int w = 0; w < weeks; w++) {
            int ago = 7 * (weeks - 1 - w);
            // максимум — на неделе 5 (30 дней назад), дальше тот же вес и столько же или меньше повторов
            boolean plateau = ago + 2 < 30;
            sessions.add(session(now, ago + 2, "Тренировка с тренером", null,
                    plateau ? main("Жим лёжа", grow(80, 2.5, 3, 2.5), w % 2 == 0 ? 5 : 6, 3)
                            : main("Жим лёжа", grow(80, 2.5, (w + 1) / 2, 2.5), 6, 3),
                    work("Тяга штанги в наклоне", grow(60, 2.5, w / 2, 2.5), 8, 3)));
            sessions.add(session(now, ago + 5, "Тренировка с тренером", null,
                    main("Приседания со штангой", grow(90, 2.5, w / 2, 2.5), 6, 3),
                    work("Жим ногами", grow(140, 5, w / 2, 5), 10, 3)));
        }
        return sessions;
    }

    /** Марат: всё растёт, но последняя тренировка 12 дней назад — "давно не был" */
    static List<Session> clientAway(Instant now) {
        return clientBase(now, 8, 12, 45, 55);
    }

    /** Анна: ровный прогресс, вчера — новый максимум в приседе */
    static List<Session> clientRecord(Instant now) {
        List<Session> sessions = clientBase(now, 10, 4, 30, 40);
        sessions.add(session(now, 1, "Тренировка с тренером", null,
                main("Приседания со штангой", 52.5, 8, 3),
                work("Жим ногами", 105, 10, 3)));
        return sessions;
    }

    // ---------- строительные блоки ----------

    /** Тренировка daysAgo суток назад (минус полтора часа), время округлено до 5 минут */
    private static Session session(Instant now, int daysAgo, String name, String template, Exercise... exercises) {
        Instant exact = now.minus(Duration.ofDays(daysAgo)).minus(BEFORE_NOW).truncatedTo(ChronoUnit.MINUTES);
        Instant start = exact.minusSeconds(exact.getEpochSecond() % 300);
        return new Session(start, name, template, List.of(exercises), false);
    }

    /** start + step за каждый шаг; шаг кратен шагу блинов/гантелей */
    static double grow(double start, double step, double steps, double round) {
        double value = start + step * Math.floor(steps);
        return Math.round(value / round) * round;
    }

    /** Основное упражнение: разминочный подход (половина веса) + рабочие */
    private static Exercise main(String name, double weight, int reps, int sets) {
        List<SetValues> values = new ArrayList<>();
        values.add(new SetValues(kg(Math.round(weight / 2 / 2.5) * 2.5), 10, null, null, null, true));
        values.addAll(sets(weight, reps, sets, RPE_NORMAL));
        return new Exercise(name, values);
    }

    /** Почти до отказа (RPE 9,5) — для недели перегрузки */
    private static Exercise hard(String name, double weight, int reps, int sets) {
        return new Exercise(name, sets(weight, reps, sets, RPE_HARD));
    }

    private static Exercise work(String name, double weight, int reps, int sets) {
        return new Exercise(name, sets(weight, reps, sets, null));
    }

    private static Exercise bodyweight(String name, int sets, int reps) {
        List<SetValues> values = new ArrayList<>();
        for (int i = 0; i < sets; i++) {
            values.add(new SetValues(null, reps - i / 2, null, null, null, false));
        }
        return new Exercise(name, values);
    }

    private static Exercise timed(String name, int sets, int seconds) {
        List<SetValues> values = new ArrayList<>();
        for (int i = 0; i < sets; i++) {
            values.add(new SetValues(null, null, seconds - i * 5, null, null, false));
        }
        return new Exercise(name, values);
    }

    /** Пробежка: одна запись — дистанция и время; paceSecPerKm — темп */
    private static Exercise run(int meters, int paceSecPerKm) {
        return new Exercise("Бег", List.of(new SetValues(null, null, meters * paceSecPerKm / 1000, meters, null, false)));
    }

    private static List<SetValues> sets(double weight, int reps, int count, BigDecimal rpe) {
        List<SetValues> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(new SetValues(kg(weight), reps, null, null, rpe, false));
        }
        return values;
    }

    private static BigDecimal kg(double weight) {
        return BigDecimal.valueOf(weight).stripTrailingZeros();
    }
}
