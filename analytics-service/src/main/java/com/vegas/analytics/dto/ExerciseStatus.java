package com.vegas.analytics.dto;

/**
 * Статус упражнения на главной (бейдж на карточке). Порядок = приоритет показа:
 * сначала то, что требует внимания.
 */
public enum ExerciseStatus {
    OVERLOAD,  // высокий риск перегрузки
    PLATEAU,   // нет роста
    RECORD,    // свежий рекорд (за последние 14 дней)
    NORMAL
}
