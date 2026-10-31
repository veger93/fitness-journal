package com.vegas.user.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DemoRateLimiterTest {

    @Test
    void slidingWindow_hourLater_allowedAgain() {
        DemoRateLimiter limiter = new DemoRateLimiter();
        Instant t = Instant.parse("2026-10-31T12:00:00Z");

        assertThat(limiter.tryAcquire("ip", 2, t)).isTrue();
        assertThat(limiter.tryAcquire("ip", 2, t.plusSeconds(60))).isTrue();
        assertThat(limiter.tryAcquire("ip", 2, t.plusSeconds(120))).isFalse();
        // первый запуск вышел из окна — место освободилось
        assertThat(limiter.tryAcquire("ip", 2, t.plus(DemoRateLimiter.WINDOW).plusSeconds(1))).isTrue();
    }
}
