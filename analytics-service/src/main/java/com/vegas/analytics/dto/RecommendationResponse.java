package com.vegas.analytics.dto;

/** Блок "Рекомендация системы". type — для логики фронта (показать кнопку плана), message — текст для пользователя. */
public record RecommendationResponse(
        RecommendationType type,
        String message
) {
}
