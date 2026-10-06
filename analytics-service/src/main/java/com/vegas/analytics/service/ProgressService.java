package com.vegas.analytics.service;

import com.vegas.analytics.dto.ExerciseProgressResponse;
import com.vegas.analytics.dto.ProgressPeriod;
import com.vegas.analytics.dto.ProgressPointResponse;
import com.vegas.analytics.dto.ReferenceResponse;
import com.vegas.analytics.entity.AthleteProfile;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.AthleteProfileRepository;
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
    /** Уровень по умолчанию, если пользователь не прошёл онбординг. */
    private static final ExperienceLevel DEFAULT_LEVEL = ExperienceLevel.INTERMEDIATE;

    private final ExercisePerformanceRepository performanceRepository;
    private final AthleteProfileRepository profileRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ExerciseProgressResponse progress(UUID userId, UUID exerciseId, ProgressPeriod period) {
        Instant now = clock.instant();
        List<ExercisePerformance> performances = performanceRepository
                .findByUserIdAndExerciseIdAndPerformedAtGreaterThanEqualOrderByPerformedAtAsc(
                        userId, exerciseId, period.startFrom(now));

        List<ExercisePerformance> withE1rm = performances.stream()
                .filter(performance -> performance.getBestE1rmKg() != null)
                .toList();
        ExercisePerformance anchor = withE1rm.isEmpty() ? null : withE1rm.get(0);
        ExercisePerformance latest = withE1rm.isEmpty() ? null : withE1rm.get(withE1rm.size() - 1);
        BigDecimal first = anchor == null ? null : anchor.getBestE1rmKg();
        BigDecimal current = latest == null ? null : latest.getBestE1rmKg();

        // все периоды не короче месяца, поэтому последняя неделя уже есть в выборке
        Instant weekAgo = now.minus(WEEK);
        BigDecimal weeklyVolume = performances.stream()
                .filter(performance -> !performance.getPerformedAt().isBefore(weekAgo))
                .map(ExercisePerformance::getVolumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String exerciseName = performances.isEmpty() ? null : performances.get(performances.size() - 1).getExerciseName();

        // эталон: стартуем из первой точки периода и растём с темпом уровня пользователя
        ExperienceLevel declared = profileRepository.findById(userId)
                .map(AthleteProfile::getExperienceLevel)
                .map(ExperienceLevel::parse)
                .orElse(null);
        ExperienceLevel level = Objects.requireNonNullElse(declared, DEFAULT_LEVEL);

        List<ProgressPointResponse> points = performances.stream()
                .map(performance -> toPoint(performance, anchor == null ? null
                        : ReferenceCurve.expected(first, anchor.getPerformedAt(), performance.getPerformedAt(),
                                level.monthlyGainPercent())))
                .toList();

        ReferenceResponse reference = latest == null ? null
                : reference(level, declared == null, current,
                ReferenceCurve.expected(first, anchor.getPerformedAt(), latest.getPerformedAt(), level.monthlyGainPercent()));

        return new ExerciseProgressResponse(
                exerciseId,
                exerciseName,
                period,
                current,
                changePercent(first, current),
                performances.isEmpty() ? null : weeklyVolume,
                reference,
                points
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

    private static ReferenceResponse reference(ExperienceLevel level, boolean assumed,
                                               BigDecimal current, BigDecimal expected) {
        BigDecimal gap = ReferenceCurve.gapPercent(current, expected);
        String pace = "уровень «" + level.label() + "»: ~" + level.monthlyGainPercent().stripTrailingZeros().toPlainString()
                + "% в месяц" + (assumed ? " (уровень не указан в профиле)" : "");
        String hint;
        if (gap.signum() >= 0) {
            hint = "идёшь быстрее обычного темпа на " + gap.abs().toPlainString() + "% — " + pace;
        } else {
            hint = "на " + gap.abs().toPlainString() + "% ниже обычного темпа — " + pace;
        }
        return new ReferenceResponse(level.name(), assumed, level.monthlyGainPercent(), gap, hint);
    }

    private static ProgressPointResponse toPoint(ExercisePerformance performance, BigDecimal reference) {
        return new ProgressPointResponse(
                performance.getPerformedAt(),
                performance.getBestE1rmKg(),
                performance.getBestWeightKg(),
                performance.getBestReps(),
                performance.getVolumeKg(),
                reference
        );
    }
}
