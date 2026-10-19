package com.vegas.analytics.service;

import com.vegas.analytics.event.AnalyticsEventsPublisher;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Раз в месяц: всем, кто тренировался в прошлом месяце, — событие "отчёт готов".
 * @Scheduled включён в common-outbox (@EnableScheduling), отдельно включать не нужно.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportNotifier {

    private final ExercisePerformanceRepository performanceRepository;
    private final MonthlyReportPublisher reportPublisher;
    private final Clock clock;

    @Scheduled(cron = "${app.reports.monthly-cron:0 0 6 1 * *}", zone = "UTC")
    public void publishForPreviousMonth() {
        YearMonth month = YearMonth.now(clock.withZone(ZoneOffset.UTC)).minusMonths(1);
        List<UUID> users = performanceRepository.findActiveUserIds(
                MonthlyReportCalculator.startOf(month), MonthlyReportCalculator.startOf(month.plusMonths(1)));
        log.info("Monthly report {}: notifying {} users", month, users.size());
        // по транзакции на пользователя + try/catch: ошибка у одного не откатит и не остановит остальных
        for (UUID userId : users) {
            try {
                reportPublisher.publish(userId, month);
            } catch (RuntimeException e) {
                log.error("Monthly report {} for user {} failed", month, userId, e);
            }
        }
    }

    /** Отдельный бин, чтобы @Transactional сработал через прокси для каждого вызова. */
    @Component
    @RequiredArgsConstructor
    public static class MonthlyReportPublisher {

        private final MonthlyReportService reportService;
        private final AnalyticsEventsPublisher eventsPublisher;

        @Transactional
        public void publish(UUID userId, YearMonth month) {
            eventsPublisher.monthlyReportReady(userId, reportService.report(userId, month));
        }
    }
}
