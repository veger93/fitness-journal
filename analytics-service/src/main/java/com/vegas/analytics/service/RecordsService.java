package com.vegas.analytics.service;

import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.dto.RecordType;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Лента личных рекордов. Рекорды не хранятся отдельно, а вычисляются из истории:
 * идём по тренировкам от старых к новым и для каждого вида рекорда помним максимум
 * по каждому упражнению. Превысили максимум — рекорд.
 *
 * Плюс подхода: удалили тренировку — рекорды пересчитаются сами, ничего не нужно "откатывать".
 * Первое выполнение упражнения рекордом не считается: сравнивать не с чем.
 * Повтор максимума (было 100, снова 100) — тоже не рекорд.
 *
 * (То же самое можно посчитать одним SQL с оконной функцией
 *  max(...) OVER (PARTITION BY exercise_id ORDER BY performed_at ROWS ... 1 PRECEDING) —
 *  вариант для больших объёмов данных.)
 */
@Service
@RequiredArgsConstructor
public class RecordsService {

    private final ExercisePerformanceRepository performanceRepository;

    @Transactional(readOnly = true)
    public List<PersonalRecordResponse> records(UUID userId, int limit) {
        List<ExercisePerformance> history = performanceRepository.findByUserIdOrderByPerformedAtAsc(userId);

        // вид рекорда -> (упражнение -> лучший результат на данный момент)
        Map<RecordType, Map<UUID, BigDecimal>> bestSoFar = new EnumMap<>(RecordType.class);
        for (RecordType type : RecordType.values()) {
            bestSoFar.put(type, new HashMap<>());
        }

        List<PersonalRecordResponse> records = new ArrayList<>();

        for (ExercisePerformance performance : history) {
            // порядок внутри одной тренировки: сначала рекорд веса, потом 1ПМ
            // (после разворота списка 1ПМ окажется выше — он важнее)
            for (RecordType type : List.of(RecordType.WEIGHT, RecordType.E1RM)) {
                BigDecimal current = valueOf(type, performance);
                if (current == null) {
                    continue; // нет такого показателя (упражнение без веса)
                }
                Map<UUID, BigDecimal> best = bestSoFar.get(type);
                BigDecimal previousBest = best.get(performance.getExerciseId());

                if (previousBest == null || current.compareTo(previousBest) > 0) {
                    if (previousBest != null) {
                        records.add(toRecord(type, performance, current, previousBest));
                    }
                    best.put(performance.getExerciseId(), current);
                }
            }
        }

        // новые сверху (List.reversed() появился только в Java 21, у нас 17)
        Collections.reverse(records);
        return records.subList(0, Math.min(limit, records.size()));
    }

    /** Значение показателя для вида рекорда. switch по enum: добавим вид — компилятор заставит дописать ветку. */
    private static BigDecimal valueOf(RecordType type, ExercisePerformance performance) {
        return switch (type) {
            case E1RM -> performance.getBestE1rmKg();
            case WEIGHT -> performance.getMaxWeightKg();
        };
    }

    private static PersonalRecordResponse toRecord(RecordType type, ExercisePerformance performance,
                                                   BigDecimal value, BigDecimal previousBest) {
        boolean weightRecord = type == RecordType.WEIGHT;
        return new PersonalRecordResponse(
                type,
                performance.getExerciseId(),
                performance.getExerciseName(),
                performance.getPerformedAt(),
                weightRecord ? performance.getMaxWeightKg() : performance.getBestWeightKg(),
                weightRecord ? performance.getMaxWeightReps() : performance.getBestReps(),
                value,
                previousBest,
                value.subtract(previousBest)
        );
    }
}
