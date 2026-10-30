package com.vegas.analytics.service;

import com.vegas.analytics.dto.ClientSignal;
import com.vegas.analytics.dto.ClientSignalResponse;
import com.vegas.analytics.dto.OverloadRiskResponse;
import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.dto.PlateauResponse;
import com.vegas.analytics.dto.RiskLevel;
import com.vegas.analytics.entity.ExercisePerformance;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Один, самый важный сигнал по клиенту. Чистая функция, как DashboardCalculator: история и "сейчас" — параметрами.
 * Плато и перегрузку считаем тем же InsightsCalculator, что и карточки на главной клиента, —
 * тренер в списке и клиент у себя видят одно и то же.
 */
public final class ClientSignalCalculator {

    /** Дольше — "давно не был" */
    static final Duration AWAY_AFTER = Duration.ofDays(7);
    /** Рекорд "свежий" для тренера неделю (на главной у клиента — две: там это повод порадоваться, тут — новость) */
    static final Duration FRESH_RECORD = Duration.ofDays(7);

    private ClientSignalCalculator() {
    }

    /**
     * @param history все выполнения клиента по возрастанию даты
     * @param latestRecord самый свежий рекорд клиента, если есть
     */
    public static ClientSignalResponse signal(UUID clientId, List<ExercisePerformance> history,
                                              Optional<PersonalRecordResponse> latestRecord, Instant now) {
        if (history.isEmpty()) {
            return new ClientSignalResponse(clientId, ClientSignal.NONE, null, null, null);
        }
        Instant lastWorkoutAt = history.get(history.size() - 1).getPerformedAt();

        // по упражнениям, сохраняя порядок; бег не берём — у него своя нагрузка (RunningService), без плато по 1ПМ
        Map<UUID, List<ExercisePerformance>> byExercise = history.stream()
                .filter(performance -> !RunningService.DISTANCE.equals(performance.getTrackingType()))
                .collect(Collectors.groupingBy(ExercisePerformance::getExerciseId, LinkedHashMap::new, Collectors.toList()));

        String overloaded = null;
        String plateauExercise = null;
        long plateauDays = 0;
        for (List<ExercisePerformance> exerciseHistory : byExercise.values()) {
            PlateauResponse plateau = InsightsCalculator.plateau(exerciseHistory, now);
            OverloadRiskResponse risk = InsightsCalculator.overloadRisk(exerciseHistory, now, plateau);
            String name = exerciseHistory.get(exerciseHistory.size() - 1).getExerciseName();
            if (risk.level() == RiskLevel.HIGH && overloaded == null) {
                overloaded = name;
            }
            // из нескольких плато показываем самое долгое
            if (plateau.detected() && plateau.daysSinceProgress() > plateauDays) {
                plateauExercise = name;
                plateauDays = plateau.daysSinceProgress();
            }
        }

        if (overloaded != null) {
            return new ClientSignalResponse(clientId, ClientSignal.OVERLOAD, overloaded, null, lastWorkoutAt);
        }
        if (plateauExercise != null) {
            return new ClientSignalResponse(clientId, ClientSignal.PLATEAU, plateauExercise, (int) plateauDays, lastWorkoutAt);
        }
        if (lastWorkoutAt.isBefore(now.minus(AWAY_AFTER))) {
            return new ClientSignalResponse(clientId, ClientSignal.AWAY, null, days(lastWorkoutAt, now), lastWorkoutAt);
        }
        return latestRecord
                .filter(record -> !record.achievedAt().isBefore(now.minus(FRESH_RECORD)))
                .map(record -> new ClientSignalResponse(clientId, ClientSignal.RECORD, record.exerciseName(),
                        days(record.achievedAt(), now), lastWorkoutAt))
                .orElseGet(() -> new ClientSignalResponse(clientId, ClientSignal.NONE, null, null, lastWorkoutAt));
    }

    /** Полных дней между моментами: вчера в это же время — 1 */
    private static int days(Instant from, Instant to) {
        return (int) Duration.between(from, to).toDays();
    }
}
