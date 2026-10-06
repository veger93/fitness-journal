package com.vegas.analytics.service;

import java.math.BigDecimal;

/**
 * Уровень подготовки и ожидаемый темп роста силы (расчётного 1ПМ) в месяц.
 *
 * Чем опытнее атлет, тем медленнее растёт сила — "закон убывающей отдачи".
 * У тренированных пауэрлифтеров сила растёт на ~10% за 1.5–2 года, т.е. около 0.5% в месяц,
 * а более слабые атлеты прибавляют примерно вдвое быстрее сильных (docs/references.md [7]).
 * Темпы для новичка и среднего уровня — ориентир проекта, их можно подстроить.
 */
public enum ExperienceLevel {
    BEGINNER("новичок", new BigDecimal("3.0")),
    INTERMEDIATE("средний", new BigDecimal("1.5")),
    ADVANCED("продвинутый", new BigDecimal("0.5"));

    private final String label;
    private final BigDecimal monthlyGainPercent;

    ExperienceLevel(String label, BigDecimal monthlyGainPercent) {
        this.label = label;
        this.monthlyGainPercent = monthlyGainPercent;
    }

    public String label() {
        return label;
    }

    public BigDecimal monthlyGainPercent() {
        return monthlyGainPercent;
    }

    /** Строка из события -> уровень. Неизвестно -> null (решает вызывающий код). */
    public static ExperienceLevel parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
