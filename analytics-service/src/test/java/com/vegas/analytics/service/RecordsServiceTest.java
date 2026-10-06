package com.vegas.analytics.service;

import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.dto.RecordItemResponse;
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
    private final UUID deadlift = UUID.randomUUID();
    private final UUID bench = UUID.randomUUID();
    private final UUID pullUps = UUID.randomUUID();

    @Test
    void heavierAndStronger_isOneCardWithTwoBadges_weightFirst() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(deadlift, "2026-09-26", "142.5", 5),
                perf(deadlift, "2026-10-01", "145", 5)));

        List<PersonalRecordResponse> cards = recordsService.records(userId, 50);

        assertThat(cards).hasSize(1);
        PersonalRecordResponse card = cards.get(0);
        assertThat(card.weightKg()).isEqualByComparingTo("145");
        assertThat(card.reps()).isEqualTo(5);
        assertThat(card.records()).extracting(RecordItemResponse::type).containsExactly(RecordType.WEIGHT, RecordType.E1RM);
        assertThat(card.records().get(0).hint()).isEqualTo("на 2.5 кг больше прошлого максимума (142.5 кг)");
        assertThat(card.records().get(1).hint()).isEqualTo("на 2.5 кг больше при тех же повторах");
    }

    @Test
    void sameWeightMoreReps_onlyStrength_withHumanHint() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "2026-10-01", "80", 6),
                perf(bench, "2026-10-04", "80", 8)));

        PersonalRecordResponse card = recordsService.records(userId, 50).get(0);

        assertThat(card.records()).singleElement().satisfies(item -> {
            assertThat(item.type()).isEqualTo(RecordType.E1RM);
            assertThat(item.hint()).isEqualTo("тот же вес, но на 2 повтора больше");
        });
    }

    @Test
    void heavierWithFewerReps_weightRecordOnly_whenStrengthDropped() {
        // 75×10 (1ПМ 100.00), потом 77×8 (1ПМ 97.53)
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "2026-10-05", "75", 10),
                perf(bench, "2026-10-06", "77", 8)));

        PersonalRecordResponse card = recordsService.records(userId, 50).get(0);

        assertThat(card.records()).singleElement().extracting(RecordItemResponse::type).isEqualTo(RecordType.WEIGHT);
        assertThat(card.weightKg()).isEqualByComparingTo("77");
    }

    @Test
    void firstAttemptDropsAndEqualValues_areNotRecords() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(bench, "2026-09-01", "80", 8),
                perf(bench, "2026-09-05", "75", 8),
                perf(bench, "2026-09-09", "80", 8)));

        assertThat(recordsService.records(userId, 50)).isEmpty();
    }

    @Test
    void bodyweightExercise_withoutWeight_isSkipped() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(pullUps, "2026-09-01", null, 10),
                perf(pullUps, "2026-09-05", null, 12)));

        assertThat(recordsService.records(userId, 50)).isEmpty();
    }

    @Test
    void newestFirst_limitCountsCards() {
        when(performanceRepository.findByUserIdOrderByPerformedAtAsc(userId)).thenReturn(List.of(
                perf(deadlift, "2026-09-01", "140", 5),
                perf(deadlift, "2026-09-05", "142.5", 5),
                perf(deadlift, "2026-09-10", "145", 5)));

        List<PersonalRecordResponse> cards = recordsService.records(userId, 1);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).weightKg()).isEqualByComparingTo("145");
        assertThat(cards.get(0).records()).hasSize(2); // limit считает карточки, бейджи внутри не режутся
    }

    @Test
    void repsWord_russianPlural() {
        assertThat(RecordHints.repsWord(1)).isEqualTo("повтор");
        assertThat(RecordHints.repsWord(3)).isEqualTo("повтора");
        assertThat(RecordHints.repsWord(5)).isEqualTo("повторов");
        assertThat(RecordHints.repsWord(12)).isEqualTo("повторов");
        assertThat(RecordHints.repsWord(21)).isEqualTo("повтор");
    }

    /** Один подход за тренировку: он же и лучший по силе, и самый тяжёлый. 1ПМ считаем по Эпли. */
    private ExercisePerformance perf(UUID exerciseId, String date, String weight, int reps) {
        BigDecimal weightKg = weight == null ? null : new BigDecimal(weight);
        return ExercisePerformance.builder()
                .userId(userId)
                .workoutId(UUID.randomUUID())
                .exerciseId(exerciseId)
                .exerciseName("Упражнение")
                .bodyPart(BodyPart.BACK)
                .trackingType("WEIGHT_REPS")
                .performedAt(Instant.parse(date + "T10:00:00Z"))
                .workingSets(3)
                .volumeKg(BigDecimal.ZERO)
                .bestWeightKg(weightKg)
                .bestReps(reps)
                .bestE1rmKg(weightKg == null ? null : OneRepMax.epley(weightKg, reps))
                .maxWeightKg(weightKg)
                .maxWeightReps(weightKg == null ? null : reps)
                .build();
    }
}
