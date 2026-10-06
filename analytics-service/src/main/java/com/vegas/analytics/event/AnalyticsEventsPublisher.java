package com.vegas.analytics.event;

import com.vegas.analytics.dto.MonthlyReportResponse;
import com.vegas.analytics.dto.OverloadRiskResponse;
import com.vegas.analytics.dto.PersonalRecordResponse;
import com.vegas.analytics.dto.PlateauResponse;
import com.vegas.analytics.dto.RiskLevel;
import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import com.vegas.analytics.service.InsightsCalculator;
import com.vegas.analytics.service.RecordsService;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.MonthlyReportReadyEvent;
import com.vegas.common.event.OverloadRiskEvent;
import com.vegas.common.event.PersonalRecordEvent;
import com.vegas.common.event.PlateauDetectedEvent;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.outbox.OutboxWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * После обработки тренировки решает, о чём стоит сообщить пользователю, и пишет события в outbox.
 * Сами письма шлёт notification-service — аналитика про email ничего не знает.
 *
 * Анти-спам:
 * 1) о старых тренировках не сообщаем (перезаливка данных, долгий простой Kafka) — notify-max-age;
 * 2) плато и риск могут повторяться на каждой тренировке — дубли отсекает notification-service
 *    по ключу (например, "плато с такой-то даты" — одно письмо на одно плато).
 */
@Slf4j
@Component
public class AnalyticsEventsPublisher {

    private final RecordsService recordsService;
    private final ExercisePerformanceRepository performanceRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;
    private final Duration notifyMaxAge;

    public AnalyticsEventsPublisher(RecordsService recordsService,
                                    ExercisePerformanceRepository performanceRepository,
                                    OutboxWriter outboxWriter,
                                    Clock clock,
                                    @Value("${app.insights.notify-max-age:7d}") Duration notifyMaxAge) {
        this.recordsService = recordsService;
        this.performanceRepository = performanceRepository;
        this.outboxWriter = outboxWriter;
        this.clock = clock;
        this.notifyMaxAge = notifyMaxAge;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void workoutRecorded(WorkoutCompletedEvent workout) {
        if (workout.completedAt().isBefore(clock.instant().minus(notifyMaxAge))) {
            log.debug("Workout {} is too old for notifications", workout.workoutId());
            return;
        }
        UUID userId = workout.userId();

        for (PersonalRecordResponse card : recordsService.recordsInWorkout(userId, workout.workoutId())) {
            write(card.exerciseId(), EventTypes.PERSONAL_RECORD, userId, new PersonalRecordEvent(
                    userId, workout.workoutId(), card.exerciseId(), card.exerciseName(), card.achievedAt(),
                    card.weightKg(), card.reps(),
                    card.records().stream()
                            .map(item -> new PersonalRecordEvent.Item(item.type().name(), item.valueKg(), item.deltaKg(), item.hint()))
                            .toList()));
        }

        for (WorkoutCompletedEvent.ExercisePerformed exercise : workout.exercises()) {
            List<ExercisePerformance> history =
                    performanceRepository.findByUserIdAndExerciseIdOrderByPerformedAtAsc(userId, exercise.exerciseId());
            if (history.isEmpty()) {
                continue;
            }
            // "сейчас" для правил — момент тренировки, а не момент обработки события
            PlateauResponse plateau = InsightsCalculator.plateau(history, workout.completedAt());
            if (plateau.detected()) {
                write(exercise.exerciseId(), EventTypes.PLATEAU_DETECTED, userId, new PlateauDetectedEvent(
                        userId, exercise.exerciseId(), exercise.exerciseName(),
                        plateau.lastProgressAt(), plateau.daysSinceProgress()));
            }
            OverloadRiskResponse risk = InsightsCalculator.overloadRisk(history, workout.completedAt(), plateau);
            if (risk.level() == RiskLevel.HIGH) {
                write(exercise.exerciseId(), EventTypes.OVERLOAD_RISK, userId, new OverloadRiskEvent(
                        userId, exercise.exerciseId(), exercise.exerciseName(), risk.level().name(),
                        risk.reasons(), workout.completedAt()));
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void monthlyReportReady(UUID userId, MonthlyReportResponse report) {
        write(userId, EventTypes.MONTHLY_REPORT_READY, userId, new MonthlyReportReadyEvent(
                userId,
                report.month(),
                report.strength() == null ? null : report.strength().changePercent(),
                report.tonnage() == null ? null : report.tonnage().after(),
                report.tonnage() == null ? null : report.tonnage().changePercent(),
                report.workouts().count(),
                report.recordsCount()));
    }

    /** Ключ сообщения = userId: все события пользователя в одной партиции, по порядку. */
    private void write(UUID aggregateId, String type, UUID userId, Object event) {
        outboxWriter.write(aggregateId, type, userId.toString(), event);
    }
}
