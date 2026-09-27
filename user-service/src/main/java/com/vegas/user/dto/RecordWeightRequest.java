package com.vegas.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** PUT /api/users/me/weight/{date} — вес за конкретный день. */
public record RecordWeightRequest(

        @NotNull(message = "вес обязателен")
        @DecimalMin(value = "20.01", message = "вес больше 20 кг")
        @DecimalMax(value = "399.99", message = "вес меньше 400 кг")
        @Digits(integer = 3, fraction = 2, message = "вес: не больше 2 знаков после запятой")
        BigDecimal weightKg
) {
}
