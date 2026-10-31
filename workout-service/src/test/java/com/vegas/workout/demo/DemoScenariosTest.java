package com.vegas.workout.demo;

import com.vegas.workout.entity.SetValues;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сценарии демо проверяем по тем же правилам, что и аналитика (InsightsCalculator), — упрощённая копия
 * формул прямо здесь: 1ПМ по Эпли, плато (21+ день без максимума и 3+ тренировки после), перегрузка
 * (объём за неделю > 1,5× среднего за 4 недели и 1ПМ на 5%+ ниже пика). Так видно, что каждый сценарий
 * даёт ровно тот сигнал, ради которого задуман, а остальные упражнения — "спокойные".
 */
class DemoScenariosTest {

    private static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    @ParameterizedTest
    @ValueSource(strings = {"ATHLETE", "COACH", "CLIENT_IN_PROGRESS", "CLIENT_OVERLOAD", "CLIENT_PLATEAU",
            "CLIENT_AWAY", "CLIENT_RECORD"})
    void everyScenario_inPast_sorted_withSets(String scenario) {
        List<DemoScenarios.Session> plan = DemoScenarios.plan(scenario, NOW);

        assertThat(plan).isNotEmpty();
        assertThat(plan).isSortedAccordingTo((a, b) -> a.start().compareTo(b.start()));
        for (DemoScenarios.Session session : plan) {
            assertThat(session.start()).isBefore(NOW);
            // завершённая тренировка не может закончиться в будущем
            if (!session.inProgress()) {
                assertThat(session.end()).isBefore(NOW);
                assertThat(session.exercises()).anyMatch(e -> !e.sets().isEmpty());
            }
        }
        assertThat(plan.stream().filter(DemoScenarios.Session::inProgress).count()).isLessThanOrEqualTo(1);
    }

    @Test
    void unknownScenario_nothing() {
        assertThat(DemoScenarios.plan("SOMETHING", NOW)).isEmpty();
    }

    @Test
    void athlete_benchRecordYesterday_squatPlateau_deadliftOverload_othersCalm() {
        Map<String, List<Point>> history = history("ATHLETE");

        // жим: вчерашняя тренировка — новый максимум
        List<Point> bench = history.get("Жим лёжа");
        assertThat(daysAgo(last(bench))).isEqualTo(1);
        assertThat(last(bench).e1rm).isGreaterThan(maxBefore(bench, bench.size() - 1));

        assertThat(plateauDays("Приседания со штангой", history)).isBetween(21L, 40L);
        assertThat(overload(history.get("Становая тяга"))).isTrue();

        // остальные упражнения с весом — без плато и без перегрузки (иначе главная пестрела бы метками)
        // свежий рекорд (14 дней) — у жима и у становой (вес перед перегрузкой), остальные — спокойные карточки
        assertThat(history.keySet().stream().filter(name -> freshRecord(history.get(name))))
                .containsExactlyInAnyOrder("Жим лёжа", "Становая тяга");
        history.forEach((name, points) -> {
            if (!name.equals("Приседания со штангой")) {
                assertThat(plateauDays(name, history)).as(name).isZero();
            }
            if (!name.equals("Становая тяга")) {
                assertThat(overload(points)).as(name).isFalse();
            }
        });
    }

    @Test
    void clients_eachHasItsSignal() {
        assertThat(overload(history("CLIENT_OVERLOAD").get("Приседания со штангой"))).isTrue();
        assertThat(plateauDays("Жим лёжа", history("CLIENT_PLATEAU"))).isBetween(21L, 40L);

        Map<String, List<Point>> away = history("CLIENT_AWAY");
        long lastWorkout = away.values().stream().mapToLong(points -> daysAgo(last(points))).min().orElseThrow();
        assertThat(lastWorkout).isEqualTo(12);
        away.keySet().forEach(name -> assertThat(plateauDays(name, away)).as(name).isZero());

        List<Point> squat = history("CLIENT_RECORD").get("Приседания со штангой");
        assertThat(daysAgo(last(squat))).isEqualTo(1);
        assertThat(last(squat).e1rm).isGreaterThan(maxBefore(squat, squat.size() - 1));

        assertThat(DemoScenarios.plan("CLIENT_IN_PROGRESS", NOW)).anyMatch(DemoScenarios.Session::inProgress);
    }

    @Test
    void coach_ownWorkouts_calm_withTwoTemplates() {
        Map<String, List<Point>> history = history("COACH");
        history.forEach((name, points) -> {
            assertThat(plateauDays(name, history)).as(name).isZero();
            assertThat(overload(points)).as(name).isFalse();
            assertThat(freshRecord(points)).as(name).isFalse();
        });
        assertThat(DemoScenarios.plan("COACH", NOW).stream().map(DemoScenarios.Session::template).distinct())
                .containsExactlyInAnyOrder(DemoScenarios.COACH_LEGS, DemoScenarios.COACH_UPPER);
    }

    // ---------- мини-аналитика для проверки ----------

    private record Point(Instant at, double e1rm, double volume) {
    }

    /** Завершённые тренировки -> по упражнению: лучший 1ПМ (рабочие подходы) и объём */
    private static Map<String, List<Point>> history(String scenario) {
        Map<String, List<Point>> result = new LinkedHashMap<>();
        for (DemoScenarios.Session session : DemoScenarios.plan(scenario, NOW)) {
            if (session.inProgress()) {
                continue;
            }
            for (DemoScenarios.Exercise exercise : session.exercises()) {
                double best = 0;
                double volume = 0;
                for (SetValues set : exercise.sets()) {
                    if (set.warmup() || set.weightKg() == null || set.reps() == null) {
                        continue;
                    }
                    double w = set.weightKg().doubleValue();
                    best = Math.max(best, set.reps() == 1 ? w : w * (1 + set.reps() / 30.0));
                    volume += w * set.reps();
                }
                if (best > 0) {
                    result.computeIfAbsent(exercise.name(), k -> new ArrayList<>()).add(new Point(session.end(), best, volume));
                }
            }
        }
        return result;
    }

    /** Дней без нового максимума, если это плато (21+ день и 3+ тренировки после); иначе 0 */
    private static long plateauDays(String name, Map<String, List<Point>> history) {
        List<Point> points = history.get(name);
        double max = 0;
        Instant progressAt = null;
        for (Point p : points) {
            if (p.e1rm > max + 1e-9) {
                max = p.e1rm;
                progressAt = p.at;
            }
        }
        Instant since = progressAt;
        long sessionsSince = points.stream().filter(p -> p.at.isAfter(since)).count();
        long days = Duration.between(progressAt, NOW).toDays();
        return points.size() >= 4 && days >= 21 && sessionsSince >= 3 ? days : 0;
    }

    /** Высокий риск: объём за неделю > 1,5× среднего за 4 недели и последний 1ПМ на 5%+ ниже пика за 4 недели */
    private static boolean overload(List<Point> points) {
        if (points.isEmpty() || points.get(0).at.isAfter(NOW.minus(Duration.ofDays(21)))) {
            return false;
        }
        double acute = volumeSince(points, NOW.minus(Duration.ofDays(7)));
        double chronic = volumeSince(points, NOW.minus(Duration.ofDays(28))) / 4;
        Point last = last(points);
        double peak = points.stream()
                .filter(p -> p != last && !p.at.isBefore(last.at.minus(Duration.ofDays(28))))
                .mapToDouble(Point::e1rm).max().orElse(last.e1rm);
        double drop = (peak - last.e1rm) / peak * 100;
        return chronic > 0 && acute / chronic > 1.5 && drop >= 5;
    }

    /** Новый максимум за последние 14 дней — бейдж "рекорд" на главной */
    private static boolean freshRecord(List<Point> points) {
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).at.isAfter(NOW.minus(Duration.ofDays(14))) && points.get(i).e1rm > maxBefore(points, i) + 1e-9) {
                return true;
            }
        }
        return false;
    }

    private static double volumeSince(List<Point> points, Instant from) {
        return points.stream().filter(p -> p.at.isAfter(from)).mapToDouble(Point::volume).sum();
    }

    private static double maxBefore(List<Point> points, int index) {
        return points.subList(0, index).stream().mapToDouble(Point::e1rm).max().orElse(0);
    }

    private static Point last(List<Point> points) {
        return points.get(points.size() - 1);
    }

    private static long daysAgo(Point point) {
        return Duration.between(point.at, NOW).toDays();
    }
}
