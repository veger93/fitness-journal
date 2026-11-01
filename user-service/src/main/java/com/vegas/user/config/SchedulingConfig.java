package com.vegas.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Включает @Scheduled: удаление истёкших демо (DemoCleanupJob) */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
