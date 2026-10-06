package com.vegas.analytics.dto;

import java.math.BigDecimal;

/** Карточка "Тренировок": сколько было в этом месяце и в прошлом. */
public record WorkoutsCountResponse(
        int count,
        int previousCount,
        BigDecimal changePercent,
        String hint
) {
}
