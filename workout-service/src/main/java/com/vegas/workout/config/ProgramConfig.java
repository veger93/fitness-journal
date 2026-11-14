package com.vegas.workout.config;

import com.vegas.workout.program.ProgramGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Генератор — чистый класс без Spring-аннотаций (его удобно тестировать через new).
 * Чтобы сервис получил его через конструктор, объявляем бин здесь.
 */
@Configuration
public class ProgramConfig {

    @Bean
    public ProgramGenerator programGenerator() {
        return new ProgramGenerator();
    }
}
