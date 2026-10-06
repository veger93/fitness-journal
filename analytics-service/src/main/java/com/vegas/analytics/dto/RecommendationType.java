package com.vegas.analytics.dto;

public enum RecommendationType {
    NOT_ENOUGH_DATA,  // мало тренировок, выводы делать рано
    KEEP_GOING,       // прогресс идёт
    WATCH_RECOVERY,   // есть отдельные признаки усталости
    DELOAD            // плато или высокий риск -> предложить план разгрузки
}
