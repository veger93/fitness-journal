package com.vegas.analytics.service;

import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.dto.RecordItemResponse;
import com.vegas.analytics.dto.RecordType;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Лента личных рекордов. Рекорды не хранятся отдельно, а вычисляются из истории:
 * идём по тренировкам от старых к новым и помним лучший результат по каждому упражнению.
 * Превысили — рекорд.
 *
 * Одна карточка = одно упражнение в одной тренировке. Если побиты и вес, и сила —
 * это одна карточка с двумя бейджами, а не две похожие записи подряд.
 *
 * Первое выполнение упражнения рекордом не считается (сравнивать не с чем),
 * повтор максимума — тоже. Удалили тренировку — рекорды пересчитаются сами.
 */
@Service
@RequiredArgsConstructor
public class RecordsService {

    private final ExercisePerformanceRepository performanceRepository;

    @Transactional(readOnly = true)
    public List<PersonalRecordResponse> records(UUID userId, int limit) {
        List<ExercisePerformance> history = performanceRepository.findByUserIdOrderByPerformedAtAsc(userId);

        // упражнение -> тренировка, где был лучший результат (по весу и по силе отдельно)
        Map<UUID, ExercisePerformance> bestByWeight = new HashMap<>();
        Map<UUID, ExercisePerformance> bestByStrength = new HashMap<>();
        List<PersonalRecordResponse> cards = new ArrayList<>();

        for (ExercisePerformance current : history) {
            UUID exerciseId = current.getExerciseId();
            List<RecordItemResponse> items = new ArrayList<>();

            weightRecord(current, bestByWeight.get(exerciseId)).ifPresent(items::add);
            strengthRecord(current, bestByStrength.get(exerciseId)).ifPresent(items::add);

            if (isBetter(current.getMaxWeightKg(), bestByWeight.get(exerciseId), ExercisePerformance::getMaxWeightKg)) {
                bestByWeight.put(exerciseId, current);
            }
            if (isBetter(current.getBestE1rmKg(), bestByStrength.get(exerciseId), ExercisePerformance::getBestE1rmKg)) {
                bestByStrength.put(exerciseId, current);
            }

            if (!items.isEmpty()) {
                RecordItemResponse headline = items.get(0); // вес идёт первым, если он есть
                cards.add(new PersonalRecordResponse(exerciseId, current.getExerciseName(), current.getPerformedAt(),
                        headline.weightKg(), headline.reps(), items));
            }
        }

        // новые сверху (List.reversed() появился только в Java 21, у нас 17)
        Collections.reverse(cards);
        return cards.subList(0, Math.min(limit, cards.size()));
    }

    private static Optional<RecordItemResponse> weightRecord(ExercisePerformance current, ExercisePerformance best) {
        BigDecimal value = current.getMaxWeightKg();
        if (value == null || best == null || value.compareTo(best.getMaxWeightKg()) <= 0) {
            return Optional.empty();
        }
        BigDecimal previous = best.getMaxWeightKg();
        BigDecimal delta = value.subtract(previous);
        return Optional.of(new RecordItemResponse(RecordType.WEIGHT,
                value, current.getMaxWeightReps(), value, previous, delta,
                RecordHints.weight(delta, previous)));
    }

    private static Optional<RecordItemResponse> strengthRecord(ExercisePerformance current, ExercisePerformance best) {
        BigDecimal value = current.getBestE1rmKg();
        if (value == null || best == null || value.compareTo(best.getBestE1rmKg()) <= 0) {
            return Optional.empty();
        }
        BigDecimal previous = best.getBestE1rmKg();
        String hint = RecordHints.strength(current.getBestWeightKg(), current.getBestReps(),
                best.getBestWeightKg(), best.getBestReps(), value);
        return Optional.of(new RecordItemResponse(RecordType.E1RM,
                current.getBestWeightKg(), current.getBestReps(), value, previous, value.subtract(previous), hint));
    }

    private static boolean isBetter(BigDecimal value, ExercisePerformance best,
                                    Function<ExercisePerformance, BigDecimal> field) {
        return value != null && (best == null || value.compareTo(field.apply(best)) > 0);
    }
}
