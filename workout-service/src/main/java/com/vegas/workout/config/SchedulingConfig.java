package com.vegas.workout.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Включает @Scheduled (нужно для OutboxPublisher). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
