package com.vegas.analytics.service;

import com.vegas.analytics.entity.ExercisePerformance;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import com.vegas.common.event.WorkoutCompletedEvent;
import com.vegas.common.event.WorkoutDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Обновление read model аналитики по событиям из Kafka. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final ExercisePerformanceRepository performanceRepository;

    /**
     * ИДЕМПОТЕНТНО: Kafka гарантирует "как минимум один раз", одно и то же событие может прийти дважды.
     * Поэтому сначала удаляем всё по этой тренировке, потом вставляем заново.
     * Обработали дважды — результат тот же, дублей нет.
     */
    @Transactional
    public void recordWorkout(WorkoutCompletedEvent event) {
        performanceRepository.deleteByWorkoutId(event.workoutId());

        List<ExercisePerformance> performances = event.exercises().stream()
                .map(exercise -> PerformanceCalculator.calculate(event, exercise))
                .flatMap(Optional::stream)
                .toList();
        performanceRepository.saveAll(performances);

        log.info("Workout {} recorded: {} exercises", event.workoutId(), performances.size());
    }

    @Transactional
    public void deleteWorkout(WorkoutDeletedEvent event) {
        int deleted = performanceRepository.deleteByWorkoutId(event.workoutId());
        log.info("Workout {} removed from analytics: {} rows", event.workoutId(), deleted);
    }
}
