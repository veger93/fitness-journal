package com.vegas.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Включает @Scheduled (напоминания). Без этой аннотации методы с @Scheduled просто не запускаются. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
