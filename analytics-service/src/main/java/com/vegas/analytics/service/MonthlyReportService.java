package com.vegas.analytics.service;

import com.vegas.analytics.dto.MonthlyReportResponse;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import com.vegas.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final ExercisePerformanceRepository performanceRepository;
    private final RecordsService recordsService;
    private final Clock clock;

    /** @param month null -> текущий месяц */
    @Transactional(readOnly = true)
    public MonthlyReportResponse report(UUID userId, YearMonth month) {
        YearMonth currentMonth = YearMonth.now(clock.withZone(ZoneOffset.UTC));
        YearMonth target = month == null ? currentMonth : month;
        if (target.isAfter(currentMonth)) {
            throw new BadRequestException("Отчёт за будущий месяц построить нельзя");
        }

        // одним запросом берём прошлый и этот месяц: прошлый нужен для "было"
        List<ExercisePerformance> history = performanceRepository
                .findByUserIdAndPerformedAtGreaterThanEqualAndPerformedAtLessThanOrderByPerformedAtAsc(
                        userId,
                        MonthlyReportCalculator.startOf(target.minusMonths(1)),
                        MonthlyReportCalculator.startOf(target.plusMonths(1)));

        int records = recordsService.countBetween(userId,
                MonthlyReportCalculator.startOf(target), MonthlyReportCalculator.startOf(target.plusMonths(1)));

        return MonthlyReportCalculator.build(target, history, records);
    }
}
