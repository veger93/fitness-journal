package com.vegas.analytics.service;

import com.vegas.analytics.dto.RunningSummaryResponse;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Достаёт пробежки и отдаёт их в RunningCalculator. */
@Service
@RequiredArgsConstructor
public class RunningService {

    static final String DISTANCE = "DISTANCE";

    private final ExercisePerformanceRepository performanceRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RunningSummaryResponse summary(UUID userId) {
        return RunningCalculator.summary(
                performanceRepository.findByUserIdAndTrackingTypeOrderByPerformedAtAsc(userId, DISTANCE), clock.instant());
    }
}
