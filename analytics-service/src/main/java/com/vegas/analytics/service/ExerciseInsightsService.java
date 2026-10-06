package com.vegas.analytics.service;

import com.vegas.analytics.dto.DeloadPlanResponse;
import com.vegas.analytics.dto.ExerciseInsightsResponse;
import com.vegas.analytics.dto.OverloadRiskResponse;
import com.vegas.analytics.dto.PlateauResponse;
import com.vegas.analytics.dto.RecommendationResponse;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import com.vegas.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Достаёт историю упражнения и отдаёт её в InsightsCalculator. Вся логика — там. */
@Service
@RequiredArgsConstructor
public class ExerciseInsightsService {

    private final ExercisePerformanceRepository performanceRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ExerciseInsightsResponse insights(UUID userId, UUID exerciseId) {
        List<ExercisePerformance> history = history(userId, exerciseId);
        Instant now = clock.instant();

        PlateauResponse plateau = InsightsCalculator.plateau(history, now);
        OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, now, plateau);
        RecommendationResponse recommendation = InsightsCalculator.recommend(history.size(), plateau, risk);

        String exerciseName = history.isEmpty() ? null : history.get(history.size() - 1).getExerciseName();
        return new ExerciseInsightsResponse(exerciseId, exerciseName, history.size(), plateau, risk, recommendation);
    }

    /** План можно сгенерировать всегда, когда есть хотя бы одна тренировка; причина — текущая рекомендация. */
    @Transactional(readOnly = true)
    public DeloadPlanResponse deloadPlan(UUID userId, UUID exerciseId) {
        List<ExercisePerformance> history = history(userId, exerciseId);
        if (history.isEmpty()) {
            throw new NotFoundException("Нет тренировок с этим упражнением — план составить не из чего");
        }
        Instant now = clock.instant();
        PlateauResponse plateau = InsightsCalculator.plateau(history, now);
        OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, now, plateau);
        String reason = InsightsCalculator.recommend(history.size(), plateau, risk).message();

        return InsightsCalculator.deloadPlan(history, reason);
    }

    private List<ExercisePerformance> history(UUID userId, UUID exerciseId) {
        return performanceRepository.findByUserIdAndExerciseIdOrderByPerformedAtAsc(userId, exerciseId);
    }
}
