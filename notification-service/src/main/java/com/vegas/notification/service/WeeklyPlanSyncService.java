package com.vegas.notification.service;

import com.vegas.common.event.WeeklyPlanUpdatedEvent;
import com.vegas.notification.entity.PlannedWorkout;
import com.vegas.notification.entity.WeeklyPlan;
import com.vegas.notification.repository.WeeklyPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Держит копию плана на неделю в актуальном состоянии по событиям из workout-service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyPlanSyncService {

    private final WeeklyPlanRepository repository;

    /**
     * Повторная доставка того же события безопасна: план просто заменится тем же самым (идемпотентно).
     * save() нужен только для нового плана; существующий Hibernate обновит сам при коммите (dirty checking).
     */
    @Transactional
    public void apply(WeeklyPlanUpdatedEvent event) {
        WeeklyPlan plan = repository.findById(event.userId())
                .orElseGet(() -> repository.save(new WeeklyPlan(event.userId())));
        List<PlannedWorkout> days = event.days().stream()
                .map(day -> new PlannedWorkout(day.dayOfWeek(), day.templateName()))
                .toList();
        if (!plan.replace(days, event.updatedAt())) {
            log.info("Skipped outdated weekly plan event for user {} ({})", event.userId(), event.updatedAt());
        }
    }

    @Transactional(readOnly = true)
    public Optional<WeeklyPlan> find(UUID userId) {
        return repository.findById(userId);
    }
}
