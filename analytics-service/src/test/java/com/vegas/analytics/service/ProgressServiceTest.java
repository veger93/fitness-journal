package com.vegas.analytics.service;

import com.vegas.analytics.dto.ExerciseProgressResponse;
import com.vegas.analytics.dto.ProgressPeriod;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    @Mock
    private ExercisePerformanceRepository performanceRepository;

    private ProgressService progressService;

    private final UUID userId = UUID.randomUUID();
    private final UUID benchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        progressService = new ProgressService(performanceRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void progress_currentChangeAndWeeklyVolume() {
        Instant threeMonthsAgo = Instant.parse("2026-07-01T12:00:00Z");
        when(performanceRepository.findByUserIdAndExerciseIdAndPerformedAtGreaterThanEqualOrderByPerformedAtAsc(
                userId, benchId, threeMonthsAgo)).thenReturn(List.of(
                perf("2026-07-10", "100", "1000"),
                perf("2026-09-20", "105", "1200"),   // больше недели назад — не в недельном объёме
                perf("2026-09-26", "108", "1300"),
                perf("2026-09-30", "110", "1500")
        ));

        ExerciseProgressResponse response = progressService.progress(userId, benchId, ProgressPeriod.M3);

        assertThat(response.exerciseName()).isEqualTo("Жим лёжа");
        assertThat(response.currentE1rmKg()).isEqualByComparingTo("110");
        assertThat(response.changePercent()).isEqualByComparingTo("10.0");     // (110 - 100) / 100
        assertThat(response.weeklyVolumeKg()).isEqualByComparingTo("2800");    // 1300 + 1500
        assertThat(response.points()).hasSize(4);
    }

    @Test
    void progress_noData_emptyState() {
        ExerciseProgressResponse response = progressService.progress(userId, benchId, ProgressPeriod.ALL);

        assertThat(response.points()).isEmpty();
        assertThat(response.currentE1rmKg()).isNull();
        assertThat(response.changePercent()).isNull();
        assertThat(response.weeklyVolumeKg()).isNull();
    }

    private ExercisePerformance perf(String date, String e1rm, String volume) {
        return ExercisePerformance.builder()
                .userId(userId)
                .workoutId(UUID.randomUUID())
                .exerciseId(benchId)
                .exerciseName("Жим лёжа")
                .bodyPart(BodyPart.CHEST)
                .trackingType("WEIGHT_REPS")
                .performedAt(Instant.parse(date + "T10:00:00Z"))
                .workingSets(3)
                .volumeKg(new BigDecimal(volume))
                .bestE1rmKg(new BigDecimal(e1rm))
                .build();
    }
}
