package com.vegas.workout.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SetResponse(
        UUID id,
        int number,          // номер подхода для экрана: 1, 2, 3...
        BigDecimal weightKg,
        Integer reps,
        Integer durationSec,
        Integer distanceM,
        BigDecimal rpe,
        boolean warmup
) {
}
