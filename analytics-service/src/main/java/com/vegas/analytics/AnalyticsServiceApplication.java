package com.vegas.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * analytics-service: 1ПМ, прогресс, эталонные кривые, плато, перетренированность, план разгрузки, рекорды, месячные отчёты.
 */
@SpringBootApplication
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
