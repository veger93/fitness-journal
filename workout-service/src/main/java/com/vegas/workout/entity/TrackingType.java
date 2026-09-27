package com.vegas.workout.entity;

/**
 * "Что записываем" в подходе. От этого зависят поля подхода и то, как считать прогресс:
 * для WEIGHT_REPS — 1ПМ и тоннаж, для TIME/DISTANCE — лучшее время/дистанция.
 */
public enum TrackingType {
    WEIGHT_REPS, // вес × повторы
    TIME,        // время (планка)
    DISTANCE     // дистанция (бег)
}
