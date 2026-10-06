package com.vegas.analytics.dto;

import java.math.BigDecimal;

/**
 * Эталон для сравнения ("Отставание от нормы").
 * level — уровень подготовки; levelAssumed = true, если пользователь его не указал и взят "средний".
 * gapPercent — на сколько % текущий 1ПМ выше (+) или ниже (−) эталонной кривой.
 * hint — пояснение для сноски мелким шрифтом.
 */
public record ReferenceResponse(
        String level,
        boolean levelAssumed,
        BigDecimal monthlyGainPercent,
        BigDecimal gapPercent,
        String hint
) {
}
