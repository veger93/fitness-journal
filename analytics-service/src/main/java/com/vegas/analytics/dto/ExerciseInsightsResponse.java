package com.vegas.analytics.dto;

import java.util.UUID;

/** Выводы по упражнению для детальной страницы: плато, риск перегрузки, рекомендация. */
public record ExerciseInsightsResponse(
        UUID exerciseId,
        String exerciseName,
        int sessionsCount,
        PlateauResponse plateau,
        OverloadRiskResponse overloadRisk,
        RecommendationResponse recommendation
) {
}
