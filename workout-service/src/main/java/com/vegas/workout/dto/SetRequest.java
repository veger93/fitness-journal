package com.vegas.workout.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Подход (модалка "Добавление подхода").
 * Здесь проверяем только диапазоны. КАКИЕ поля обязательны, зависит от упражнения
 * (вес×повторы / время / дистанция) — это проверяет сервис.
 *
 * clientId — id подхода, который придумал телефон (UUID). Повторный запрос с тем же clientId не создаёт
 * второй подход, а возвращает сохранённый (идемпотентность). Необязательный: старые клиенты его не шлют.
 */
public record SetRequest(

        @DecimalMin(value = "0", message = "вес не может быть отрицательным")
        @DecimalMax(value = "9999.99", message = "слишком большой вес")
        @Digits(integer = 4, fraction = 2, message = "вес: не больше 2 знаков после запятой")
        BigDecimal weightKg,

        @Min(value = 1, message = "повторов минимум 1")
        @Max(value = 1000, message = "повторов не больше 1000")
        Integer reps,

        @Min(value = 1, message = "время минимум 1 секунда")
        @Max(value = 86_400, message = "время не больше суток")
        Integer durationSec,

        @Min(value = 1, message = "дистанция минимум 1 метр")
        @Max(value = 1_000_000, message = "дистанция не больше 1000 км")
        Integer distanceM,

        @DecimalMin(value = "1", message = "RPE от 1 до 10")
        @DecimalMax(value = "10", message = "RPE от 1 до 10")
        @Digits(integer = 2, fraction = 1, message = "RPE: один знак после запятой")
        BigDecimal rpe,

        Boolean warmup,

        UUID clientId
) {
}
