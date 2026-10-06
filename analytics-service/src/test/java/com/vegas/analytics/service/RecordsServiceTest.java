package com.vegas.analytics.service;

import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.dto.RecordType;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordsServiceTest {

    @Mock
    private ExercisePerformanceRepository performanceRepository;

    @InjectMocks
    private RecordsService recordsService;

    private final UUID userId = UUID.randomUUID();
    private final UUID bench = UUID.randomUUID();
    private final UUID squat = UUID.randomUUID();
    private final UUID pullUps = UUID.randomUUID();

    @Test
    void heavierWeightWithLowerE1rm_isWeightRecordOnly() {
        // ровно твой случай: 75×10 (1ПМ 100.00), потом 77×8 (1ПМ 97.53)
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "2026-10-05", "75", 10, "100.00"),
                perf(bench, "2026-10-06", "77", 8, "97.53")
        ));

        List<PersonalRecordResponse> records = recordsService.records(userId, 50);

        assertThat(records).hasSize(1);
        PersonalRecordResponse record = records.get(0);
        assertThat(record.type()).isEqualTo(RecordType.WEIGHT);
        assertThat(record.weightKg()).isEqualByComparingTo("77");
        assertThat(record.reps()).isEqualTo(8);
        assertThat(record.previousValueKg()).isEqualByComparingTo("75");
        assertThat(record.deltaKg()).isEqualByComparingTo("2");
    }

    @Test
    void sameWeightMoreReps_isE1rmRecordOnly() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "2026-10-05", "80", 6, "96.00"),
                perf(bench, "2026-10-08", "80", 8, "101.33")
        ));

        List<PersonalRecordResponse> records = recordsService.records(userId, 50);

        assertThat(records).extracting(PersonalRecordResponse::type).containsExactly(RecordType.E1RM);
        assertThat(records.get(0).deltaKg()).isEqualByComparingTo("5.33");
    }

    @Test
    void heavierAndStronger_givesBothRecords_e1rmFirst() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(squat, "2026-10-01", "100", 5, "116.67"),
                perf(squat, "2026-10-04", "110", 5, "128.33")
        ));

        List<PersonalRecordResponse> records = recordsService.records(userId, 50);

        assertThat(records).extracting(PersonalRecordResponse::type)
                .containsExactly(RecordType.E1RM, RecordType.WEIGHT);
    }

    @Test
    void firstAttemptDropsAndEqualValues_areNotRecords() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "2026-09-01", "80", 8, "101.33"),   // первое — не рекорд
                perf(bench, "2026-09-05", "75", 8, "95.00"),    // спад
                perf(bench, "2026-09-09", "80", 8, "101.33")    // повтор максимума
        ));

        assertThat(recordsService.records(userId, 50)).isEmpty();
    }

    @Test
    void bodyweightExercise_withoutWeight_isSkipped() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(pullUps, "2026-09-01", null, 10, null),
                perf(pullUps, "2026-09-05", null, 12, null)
        ));

        assertThat(recordsService.records(userId, 50)).isEmpty();
    }

    @Test
    void newestFirst_andLimitApplied() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "2026-09-01", "80", 8, "101.33"),
                perf(bench, "2026-09-05", "80", 9, "104.00"),
                perf(bench, "2026-09-10", "80", 10, "106.67")
        ));

        List<PersonalRecordResponse> records = recordsService.records(userId, 1);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).valueKg()).isEqualByComparingTo("106.67");
    }

    /** Один подход за тренировку: он же и лучший по 1ПМ, и самый тяжёлый. */
    private ExercisePerformance perf(UUID exerciseId, String date, String weight, int reps, String e1rm) {
        BigDecimal weightKg = weight == null ? null : new BigDecimal(weight);
        return ExercisePerformance.builder()
                .userId(userId)
                .workoutId(UUID.randomUUID())
                .exerciseId(exerciseId)
                .exerciseName("Упражнение")
                .bodyPart(BodyPart.CHEST)
                .trackingType("WEIGHT_REPS")
                .performedAt(Instant.parse(date + "T10:00:00Z"))
                .workingSets(3)
                .volumeKg(BigDecimal.ZERO)
                .bestWeightKg(weightKg)
                .bestReps(reps)
                .bestE1rmKg(e1rm == null ? null : new BigDecimal(e1rm))
                .maxWeightKg(weightKg)
                .maxWeightReps(weightKg == null ? null : reps)
                .build();
    }
}
