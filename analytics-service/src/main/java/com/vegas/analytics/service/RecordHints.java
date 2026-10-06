package com.vegas.analytics.service;

import java.math.BigDecimal;

/**
 * Пояснения к рекордам на языке спортсмена, без формул.
 * Спортсмену не важно, как считается 1ПМ, — ему важно "что я сделал лучше, чем раньше".
 */
final class RecordHints {

    private RecordHints() {
    }

    static String weight(BigDecimal delta, BigDecimal previous) {
        return "на " + kg(delta) + " кг больше прошлого максимума (" + kg(previous) + " кг)";
    }

    /** Сравниваем подход-рекорд с прошлым лучшим подходом: что именно стало лучше. */
    static String strength(BigDecimal weight, int reps, BigDecimal previousWeight, Integer previousReps, BigDecimal e1rm) {
        if (previousWeight == null || previousReps == null) {
            return "≈ " + kg(e1rm) + " кг, если бы делал 1 повтор";
        }
        int weightCompare = weight.compareTo(previousWeight);
        int repsDiff = reps - previousReps;

        if (weightCompare == 0 && repsDiff > 0) {
            return "тот же вес, но на " + repsDiff + " " + repsWord(repsDiff) + " больше";
        }
        if (weightCompare > 0 && repsDiff == 0) {
            return "на " + kg(weight.subtract(previousWeight)) + " кг больше при тех же повторах";
        }
        if (weightCompare > 0 && repsDiff > 0) {
            return "больше и вес, и повторы";
        }
        if (weightCompare > 0) {
            return "вес выше на " + kg(weight.subtract(previousWeight)) + " кг, повторов чуть меньше — в сумме сильнее";
        }
        return "вес меньше, но повторов заметно больше — в сумме сильнее";
    }

    /** 1 повтор, 2 повтора, 5 повторов, 11 повторов, 21 повтор. */
    static String repsWord(int n) {
        int lastTwo = Math.abs(n) % 100;
        int last = lastTwo % 10;
        if (lastTwo >= 11 && lastTwo <= 14) {
            return "повторов";
        }
        if (last == 1) {
            return "повтор";
        }
        if (last >= 2 && last <= 4) {
            return "повтора";
        }
        return "повторов";
    }

    /** 2.50 -> "2.5", 145.00 -> "145". */
    private static String kg(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
