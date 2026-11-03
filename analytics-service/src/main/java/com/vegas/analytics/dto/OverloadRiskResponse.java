package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Риск перегрузки по одному упражнению — оценка по косвенным признакам, а не диагноз.
 * acuteChronicRatio — объём за 7 дней / средний недельный объём за 28 дней (null, если истории мало);
 * e1rmDropPercent — насколько 1ПМ ниже пика за 4 недели, подтверждённое двумя тренировками подряд (меньшее из двух);
 * recentAvgRpe — средний RPE за 7 дней (null, если не указывали);
 * reasons — человекочитаемые причины, их показываем пользователю.
 */
public record OverloadRiskResponse(
        RiskLevel level,
        BigDecimal acuteChronicRatio,
        BigDecimal e1rmDropPercent,
        BigDecimal recentAvgRpe,
        List<String> reasons
) {
}
