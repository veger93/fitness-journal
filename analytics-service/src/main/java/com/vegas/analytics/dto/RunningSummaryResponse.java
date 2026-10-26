package com.vegas.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Страница "Бег": километраж по неделям, нагрузка, рекорды и баланс лёгких/тяжёлых пробежек.
 * Считается по всем упражнениям на дистанцию (дорожка и улица — одно и то же для ног).
 * empty = true — пробежек ещё не было.
 */
public record RunningSummaryResponse(
        boolean empty,
        List<Week> weeks,
        Load load,
        Run longestRun,
        Run bestPace,
        Effort effort
) {
    /** Неделя с понедельника (UTC) и сколько пробежано, метров. */
    public record Week(LocalDate weekStart, int distanceM) {
    }

    /**
     * Рост километража: последние 14 дней против 14 дней до этого.
     * warning — рост больше 30% (Nielsen et al., 2014; у начинающих бегунов — возможный риск травм).
     * hint — пояснение простыми словами, null если сказать нечего.
     */
    public record Load(int lastTwoWeeksM, int previousTwoWeeksM, BigDecimal changePercent, boolean warning, String hint) {
    }

    /** Одна пробежка-рекорд. fresh — поставлен за последние 14 дней (и это не первая пробежка вообще). */
    public record Run(Instant date, Integer distanceM, Integer durationSec, BigDecimal paceSecPerKm, boolean fresh) {
    }

    /**
     * Как давались пробежки за 28 дней — по отметке "Как далась пробежка?" (RPE).
     * hint — если лёгких меньше половины: у бегунов основа — лёгкий бег (Seiler, 2010).
     */
    public record Effort(int easy, int moderate, int hard, int unrated, String hint) {
    }
}
