package com.vegas.analytics.service;

import com.vegas.analytics.dto.ClientSignal;
import com.vegas.analytics.dto.ClientSignalResponse;
import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientSignalCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final UUID CLIENT = UUID.randomUUID();
    private static final UUID SQUAT = UUID.randomUUID();
    private static final UUID BENCH = UUID.randomUUID();
    private static final UUID CURL = UUID.randomUUID();

    @Test
    void noWorkouts_none() {
        ClientSignalResponse signal = ClientSignalCalculator.signal(CLIENT, List.of(), Optional.empty(), NOW);

        assertThat(signal.signal()).isEqualTo(ClientSignal.NONE);
        assertThat(signal.lastWorkoutAt()).isNull();
    }

    @Test
    void overload_beatsPlateauAndRecord() {
        List<ExercisePerformance> history = new ArrayList<>(overloadedSquat());
        history.addAll(plateauBench());

        ClientSignalResponse signal = ClientSignalCalculator.signal(CLIENT, sorted(history), Optional.of(record(1)), NOW);

        assertThat(signal.signal()).isEqualTo(ClientSignal.OVERLOAD);
        assertThat(signal.exerciseName()).isEqualTo("Приседания");
    }

    @Test
    void plateau_withDays() {
        ClientSignalResponse signal = ClientSignalCalculator.signal(CLIENT, sorted(plateauBench()), Optional.of(record(1)), NOW);

        assertThat(signal.signal()).isEqualTo(ClientSignal.PLATEAU);
        assertThat(signal.exerciseName()).isEqualTo("Жим");
        assertThat(signal.days()).isEqualTo(30);
    }

    @Test
    void away_afterAWeekWithoutWorkouts() {
        List<ExercisePerformance> history = List.of(perf(CURL, "Сгибания", 12, "40", "300"));

        ClientSignalResponse signal = ClientSignalCalculator.signal(CLIENT, history, Optional.empty(), NOW);

        assertThat(signal.signal()).isEqualTo(ClientSignal.AWAY);
        assertThat(signal.days()).isEqualTo(12);
    }

    @Test
    void freshRecord_whenNothingWorse() {
        List<ExercisePerformance> history = List.of(perf(CURL, "Сгибания", 1, "40", "300"));

        ClientSignalResponse signal = ClientSignalCalculator.signal(CLIENT, history, Optional.of(record(1)), NOW);

        assertThat(signal.signal()).isEqualTo(ClientSignal.RECORD);
        assertThat(signal.exerciseName()).isEqualTo("Становая");
        assertThat(signal.days()).isEqualTo(1);
    }

    @Test
    void oldRecord_none() {
        List<ExercisePerformance> history = List.of(perf(CURL, "Сгибания", 1, "40", "300"));

        ClientSignalResponse signal = ClientSignalCalculator.signal(CLIENT, history, Optional.of(record(10)), NOW);

        assertThat(signal.signal()).isEqualTo(ClientSignal.NONE);
        assertThat(signal.lastWorkoutAt()).isEqualTo(NOW.minus(Duration.ofDays(1)));
    }

    /** Как в DashboardCalculatorTest: объём резко вырос, сила упала -> высокий риск */
    private static List<ExercisePerformance> overloadedSquat() {
        return List.of(
                perf(SQUAT, "Приседания", 25, "110", "1000"),
                perf(SQUAT, "Приседания", 18, "110", "1000"),
                perf(SQUAT, "Приседания", 11, "108", "1000"),
                perf(SQUAT, "Приседания", 3, "100", "2500"));
    }

    /** Максимум 30 дней назад, потом три тренировки без роста -> плато */
    private static List<ExercisePerformance> plateauBench() {
        return List.of(
                perf(BENCH, "Жим", 35, "95", "1000"),
                perf(BENCH, "Жим", 30, "100", "1000"),
                perf(BENCH, "Жим", 20, "99", "1000"),
                perf(BENCH, "Жим", 10, "98", "1000"),
                perf(BENCH, "Жим", 4, "99", "1000"));
    }

    private static List<ExercisePerformance> sorted(List<ExercisePerformance> history) {
        return history.stream().sorted(Comparator.comparing(ExercisePerformance::getPerformedAt)).toList();
    }

    private static ExercisePerformance perf(UUID exerciseId, String name, int daysAgo, String e1rm, String volume) {
        return ExercisePerformance.builder()
                .userId(CLIENT)
                .workoutId(UUID.randomUUID())
                .exerciseId(exerciseId)
                .exerciseName(name)
                .bodyPart(BodyPart.CHEST)
                .trackingType("WEIGHT_REPS")
                .performedAt(NOW.minus(Duration.ofDays(daysAgo)))
                .workingSets(3)
                .volumeKg(new BigDecimal(volume))
                .bestE1rmKg(new BigDecimal(e1rm))
                .maxWeightKg(new BigDecimal("80"))
                .maxWeightReps(5)
                .build();
    }

    private static PersonalRecordResponse record(int daysAgo) {
        return new PersonalRecordResponse(UUID.randomUUID(), UUID.randomUUID(), "Становая",
                NOW.minus(Duration.ofDays(daysAgo)), new BigDecimal("150"), 3, List.of());
    }
}
