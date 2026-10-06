package com.vegas.analytics.service;

import com.vegas.analytics.repository.ExercisePerformanceRepository;
import com.vegas.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonthlyReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");

    @Mock
    private ExercisePerformanceRepository performanceRepository;
    @Mock
    private RecordsService recordsService;

    private MonthlyReportService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MonthlyReportService(performanceRepository, recordsService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void noMonthGiven_currentMonth_loadsPreviousAndCurrent() {
        var report = service.report(userId, null);

        assertThat(report.month()).isEqualTo("2026-10");
        verify(performanceRepository).findByUserIdAndPerformedAtGreaterThanEqualAndPerformedAtLessThanOrderByPerformedAtAsc(
                userId, Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"));
    }

    @Test
    void futureMonth_isRejected() {
        assertThatThrownBy(() -> service.report(userId, YearMonth.of(2026, 11)))
                .isInstanceOf(BadRequestException.class);
    }
}
