package com.vegas.analytics.service;

import com.vegas.analytics.dto.PersonalRecordResponse;
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

    @Test
    void recordIsANewMaximum_firstAttemptAndDropsAreNot() {
        when(performanceRepository.findByUserIdAndBestE1rmKgIsNotNullOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "Жим лёжа", "2026-09-01", "100"),       // первое выполнение — не рекорд
                perf(squat, "Присед", "2026-09-02", "120"),        // первое выполнение — не рекорд
                perf(bench, "Жим лёжа", "2026-09-05", "105"),       // рекорд +5
                perf(bench, "Жим лёжа", "2026-09-08", "103"),       // спад — не рекорд
                perf(bench, "Жим лёжа", "2026-09-12", "105"),       // повтор максимума — не рекорд
                perf(squat, "Присед", "2026-09-13", "127.5")       // рекорд +7.5
        ));

        List<PersonalRecordResponse> records = recordsService.records(userId, 50);

        assertThat(records).hasSize(2);
        // новые сверху
        assertThat(records.get(0).exerciseName()).isEqualTo("Присед");
        assertThat(records.get(0).deltaKg()).isEqualByComparingTo("7.5");
        assertThat(records.get(1).exerciseName()).isEqualTo("Жим лёжа");
        assertThat(records.get(1).previousE1rmKg()).isEqualByComparingTo("100");
        assertThat(records.get(1).deltaKg()).isEqualByComparingTo("5");
    }

    @Test
    void limitIsApplied() {
        when(performanceRepository.findByUserIdAndBestE1rmKgIsNotNullOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "Жим лёжа", "2026-09-01", "100"),
                perf(bench, "Жим лёжа", "2026-09-05", "102"),
                perf(bench, "Жим лёжа", "2026-09-10", "104")
        ));

        List<PersonalRecordResponse> records = recordsService.records(userId, 1);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).e1rmKg()).isEqualByComparingTo("104");
    }

    private ExercisePerformance perf(UUID exerciseId, String name, String date, String e1rm) {
        return ExercisePerformance.builder()
                .userId(userId)
                .workoutId(UUID.randomUUID())
                .exerciseId(exerciseId)
                .exerciseName(name)
                .bodyPart(BodyPart.CHEST)
                .trackingType("WEIGHT_REPS")
                .performedAt(Instant.parse(date + "T10:00:00Z"))
                .workingSets(3)
                .volumeKg(BigDecimal.ZERO)
                .bestWeightKg(new BigDecimal("80"))
                .bestReps(8)
                .bestE1rmKg(new BigDecimal(e1rm))
                .build();
    }
}
