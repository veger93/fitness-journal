package com.vegas.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * "Сейчас" как зависимость. Сервисы вызывают LocalDate.now(clock), а не LocalDate.now().
 * Зачем: в тестах подставляем Clock.fixed(...) и получаем предсказуемую "сегодняшнюю" дату.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
