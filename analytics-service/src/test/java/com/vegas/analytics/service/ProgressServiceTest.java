package com.vegas.analytics.service;

import com.vegas.analytics.dto.ExerciseProgressResponse;
import com.vegas.analytics.dto.ProgressPeriod;
import com.vegas.analytics.entity.AthleteProfile;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.AthleteProfileRepository;
import com.vegas.common.event.UserProfileUpdatedEvent;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    @Mock
    private ExercisePerformanceRepository performanceRepository;
    @Mock
    private AthleteProfileRepository profileRepository;

    private ProgressService progressService;

    private final UUID userId = UUID.randomUUID();
    private final UUID benchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        progressService = new ProgressService(performanceRepository, profileRepository, Clock.fixed(NOW, ZoneOffset.UTC));
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

        // профиля нет -> уровень "средний" (1.5% в месяц), стартуем от 100 кг 10 июля
        assertThat(response.reference().level()).isEqualTo("INTERMEDIATE");
        assertThat(response.reference().levelAssumed()).isTrue();
        assertThat(response.points().get(0).referenceE1rmKg()).isEqualByComparingTo("100");
        assertThat(response.points().get(1).referenceE1rmKg()).isEqualByComparingTo("103.58");
        // 30 сентября эталон 104.09, факт 110 -> на 5.7% быстрее
        assertThat(response.reference().gapPercent()).isEqualByComparingTo("5.7");
        assertThat(response.reference().hint()).startsWith("идёшь быстрее обычного темпа на 5.7%");
    }

    @Test
    void reference_usesLevelFromProfile() {
        Instant threeMonthsAgo = Instant.parse("2026-07-01T12:00:00Z");
        when(performanceRepository.findByUserIdAndExerciseIdAndPerformedAtGreaterThanEqualOrderByPerformedAtAsc(
                userId, benchId, threeMonthsAgo)).thenReturn(List.of(
                perf("2026-07-10", "100", "1000"),
                perf("2026-09-30", "100", "1000")));   // 1ПМ не вырос
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile("ADVANCED")));

        ExerciseProgressResponse response = progressService.progress(userId, benchId, ProgressPeriod.M3);

        // продвинутый: 0.5% в месяц -> эталон 101.35, факт 100 -> отстаём на 1.3%
        assertThat(response.reference().levelAssumed()).isFalse();
        assertThat(response.reference().gapPercent()).isEqualByComparingTo("-1.3");
        assertThat(response.reference().hint()).startsWith("на 1.3% ниже обычного темпа");
    }

    @Test
    void progress_noData_emptyState() {
        ExerciseProgressResponse response = progressService.progress(userId, benchId, ProgressPeriod.ALL);

        assertThat(response.points()).isEmpty();
        assertThat(response.currentE1rmKg()).isNull();
        assertThat(response.changePercent()).isNull();
        assertThat(response.weeklyVolumeKg()).isNull();
        assertThat(response.reference()).isNull();
    }

    private AthleteProfile profile(String level) {
        return AthleteProfile.from(new UserProfileUpdatedEvent(userId, null, 1995, 178, level,
                new BigDecimal("80"), Instant.parse("2026-09-01T00:00:00Z")));
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
