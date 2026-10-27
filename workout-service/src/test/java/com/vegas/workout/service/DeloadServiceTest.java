package com.vegas.workout.service;

import com.vegas.common.exception.NotFoundException;
import com.vegas.common.model.BodyPart;
import com.vegas.workout.dto.DeloadRequest;
import com.vegas.workout.dto.DeloadResponse;
import com.vegas.workout.entity.DeloadPeriod;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.TrackingType;
import com.vegas.workout.repository.DeloadPeriodRepository;
import com.vegas.workout.repository.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeloadServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");

    @Mock
    private DeloadPeriodRepository deloadRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    private DeloadService service;

    private final UUID me = UUID.randomUUID();
    private final UUID squatId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DeloadService(deloadRepository, exerciseRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void start_endsPreviousAndCreatesNewForGivenDays() {
        when(exerciseRepository.findById(squatId)).thenReturn(Optional.of(myExercise()));
        DeloadPeriod previous = new DeloadPeriod(me, squatId, new BigDecimal("60"), 3, 5,
                NOW.minus(Duration.ofDays(2)), NOW.plus(Duration.ofDays(5)));
        when(deloadRepository.findByUserIdAndExerciseIdAndEndsAtAfter(me, squatId, NOW)).thenReturn(List.of(previous));
        when(deloadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DeloadResponse started = service.start(me, squatId, new DeloadRequest(new BigDecimal("70"), 3, 5, 7));

        // активной может быть только одна: старую закрыли "сейчас"
        assertThat(previous.getEndsAt()).isEqualTo(NOW);
        assertThat(started.workingWeightKg()).isEqualByComparingTo("70");
        assertThat(started.endsAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void start_foreignExercise_isNotFound() {
        Exercise foreign = Exercise.custom(UUID.randomUUID(), "Чужое", BodyPart.LEGS, Equipment.BARBELL,
                TrackingType.WEIGHT_REPS, null, null);
        when(exerciseRepository.findById(squatId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.start(me, squatId, new DeloadRequest(null, 3, 5, 7)))
                .isInstanceOf(NotFoundException.class);
        verify(deloadRepository, never()).save(any());
    }

    @Test
    void stop_endsActiveNow() {
        DeloadPeriod active = new DeloadPeriod(me, squatId, null, 3, 5, NOW.minus(Duration.ofDays(1)),
                NOW.plus(Duration.ofDays(6)));
        when(deloadRepository.findByUserIdAndExerciseIdAndEndsAtAfter(me, squatId, NOW)).thenReturn(List.of(active));

        service.stop(me, squatId);

        assertThat(active.isActiveAt(NOW)).isFalse();
    }

    private Exercise myExercise() {
        return Exercise.custom(me, "Присед", BodyPart.LEGS, Equipment.BARBELL, TrackingType.WEIGHT_REPS, null, null);
    }
}
