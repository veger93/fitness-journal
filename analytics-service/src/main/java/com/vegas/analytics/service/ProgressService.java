package com.vegas.analytics.service;

import com.vegas.analytics.dto.ExerciseProgressResponse;
import com.vegas.analytics.dto.ProgressPeriod;
import com.vegas.analytics.dto.ProgressPointResponse;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final Duration WEEK = Duration.ofDays(7);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ExercisePerformanceRepository performanceRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ExerciseProgressResponse progress(UUID userId, UUID exerciseId, ProgressPeriod period) {
        Instant now = clock.instant();
        List<ExercisePerformance> performances = performanceRepository
                .findByUserIdAndExerciseIdAndPerformedAtGreaterThanEqualOrderByPerformedAtAsc(
                        userId, exerciseId, period.startFrom(now));

        List<BigDecimal> e1rms = performances.stream()
                .map(ExercisePerformance::getBestE1rmKg)
                .filter(Objects::nonNull)
                .toList();
        BigDecimal first = e1rms.isEmpty() ? null : e1rms.get(0);
        BigDecimal current = e1rms.isEmpty() ? null : e1rms.get(e1rms.size() - 1);

        // все периоды не короче месяца, поэтому последняя неделя уже есть в выборке
        Instant weekAgo = now.minus(WEEK);
        BigDecimal weeklyVolume = performances.stream()
                .filter(performance -> !performance.getPerformedAt().isBefore(weekAgo))
                .map(ExercisePerformance::getVolumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String exerciseName = performances.isEmpty() ? null : performances.get(performances.size() - 1).getExerciseName();

        return new ExerciseProgressResponse(
                exerciseId,
                exerciseName,
                period,
                current,
                changePercent(first, current),
                performances.isEmpty() ? null : weeklyVolume,
                performances.stream().map(ProgressService::toPoint).toList()
        );
    }

    /** (стало - было) / было × 100, один знак после запятой. Нет данных или было 0 -> null. */
    static BigDecimal changePercent(BigDecimal first, BigDecimal current) {
        if (first == null || current == null || first.signum() == 0) {
            return null;
        }
        return current.subtract(first)
                .multiply(HUNDRED)
                .divide(first, 1, RoundingMode.HALF_UP);
    }

    private static ProgressPointResponse toPoint(ExercisePerformance performance) {
        return new ProgressPointResponse(
                performance.getPerformedAt(),
                performance.getBestE1rmKg(),
                performance.getBestWeightKg(),
                performance.getBestReps(),
                performance.getVolumeKg()
        );
    }
}
