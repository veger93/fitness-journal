package com.vegas.analytics.service;

import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Лента личных рекордов. Рекорды не хранятся отдельно, а вычисляются из истории:
 * идём по тренировкам от старых к новым и запоминаем лучший 1ПМ по каждому упражнению.
 * Превысили предыдущий максимум — это рекорд.
 *
 * Плюс подхода: удалили тренировку — рекорды пересчитаются сами, ничего не нужно "откатывать".
 * Первое выполнение упражнения рекордом не считается: сравнивать не с чем.
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
        List<ExercisePerformance> history =
                performanceRepository.findByUserIdAndBestE1rmKgIsNotNullOrderByPerformedAtAsc(userId);

        Map<UUID, BigDecimal> bestSoFar = new HashMap<>();
        List<PersonalRecordResponse> records = new ArrayList<>();

        for (ExercisePerformance performance : history) {
            BigDecimal previousBest = bestSoFar.get(performance.getExerciseId());
            BigDecimal current = performance.getBestE1rmKg();

            if (previousBest == null || current.compareTo(previousBest) > 0) {
                if (previousBest != null) {
                    records.add(toRecord(performance, previousBest));
                }
                bestSoFar.put(performance.getExerciseId(), current);
            }
        }

        // новые сверху (List.reversed() появился только в Java 21, у нас 17)
        Collections.reverse(records);
        return records.subList(0, Math.min(limit, records.size()));
    }

    private static PersonalRecordResponse toRecord(ExercisePerformance performance, BigDecimal previousBest) {
        return new PersonalRecordResponse(
                performance.getExerciseId(),
                performance.getExerciseName(),
                performance.getPerformedAt(),
                performance.getBestWeightKg(),
                performance.getBestReps(),
                performance.getBestE1rmKg(),
                previousBest,
                performance.getBestE1rmKg().subtract(previousBest)
        );
    }
}
