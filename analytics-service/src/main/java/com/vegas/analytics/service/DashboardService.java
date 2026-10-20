package com.vegas.analytics.service;

import com.vegas.analytics.dto.DashboardResponse;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Главная одним запросом, а не десятком с фронта (по запросу на каждое упражнение).
 * Берём всю историю пользователя: для одного человека это сотни-тысячи строк — нормально.
 * Если станет тяжело — ограничим историю последними месяцами (плато и риск смотрят максимум на 4 недели назад
 * плюс точку последнего максимума).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int ALL_RECORDS = Integer.MAX_VALUE;

    private final ExercisePerformanceRepository performanceRepository;
    private final RecordsService recordsService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID userId) {
        return DashboardCalculator.build(
                performanceRepository.findByUserIdOrderByPerformedAtAsc(userId),
                recordsService.records(userId, ALL_RECORDS),
                clock.instant());
    }
}
