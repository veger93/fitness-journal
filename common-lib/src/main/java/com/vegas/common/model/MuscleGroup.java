package com.vegas.common.model;

/**
 * Группы мышц. Закладываем СРАЗУ, т.к. на них завязаны:
 * — фильтры в каталоге упражнений;
 * — подсветка мышц на карте тела (id на фронте = name() этого enum);
 * — аналитика объёма по группам мышц.
 * Не переименовывай значения после появления данных в БД — они хранятся строкой.
 */
public enum MuscleGroup {
    CHEST,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    OBLIQUES,
    UPPER_BACK,
    LATS,
    LOWER_BACK,
    TRAPS,
    GLUTES,
    QUADS,
    HAMSTRINGS,
    ADDUCTORS,
    CALVES
}
