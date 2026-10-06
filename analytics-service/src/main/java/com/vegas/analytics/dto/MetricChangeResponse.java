package com.vegas.analytics.dto;

import java.math.BigDecimal;

/**
 * Показатель "было -> стало" (карточки "Сила в среднем", "Тоннаж").
 * changePercent = null, если сравнивать не с чем (в прошлом месяце не было данных).
 * hint — пояснение для сноски мелким шрифтом.
 */
public record MetricChangeResponse(
        BigDecimal before,
        BigDecimal after,
        BigDecimal changePercent,
        String hint
) {
}
