package com.vegas.notification.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Копия плана на неделю пользователя (см. миграцию 006).
 *
 * @ElementCollection — список значений в отдельной таблице weekly_plan_days. Hibernate сам удаляет и вставляет строки,
 * когда список меняется: отдельный репозиторий для дней не нужен.
 * EAGER: план — максимум 7 строк, и он всегда нужен целиком.
 */
@Entity
@Table(name = "weekly_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyPlan {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "weekly_plan_days", joinColumns = @JoinColumn(name = "user_id"))
    private List<PlannedWorkout> days = new ArrayList<>();

    public WeeklyPlan(UUID userId) {
        this.userId = userId;
        this.updatedAt = Instant.EPOCH; // любое настоящее событие новее
    }

    /**
     * Заменить план целиком. Событие старее сохранённого — пропускаем: Kafka сохраняет порядок внутри партиции,
     * но при повторной доставке старое сообщение может прийти после нового.
     *
     * @return true, если план изменился
     */
    public boolean replace(List<PlannedWorkout> newDays, Instant eventTime) {
        if (eventTime.isBefore(updatedAt)) {
            return false;
        }
        days.clear();
        days.addAll(newDays);
        updatedAt = eventTime;
        return true;
    }

    /** Название комплекса на этот день; empty — день отдыха */
    public Optional<String> templateFor(DayOfWeek day) {
        return days.stream().filter(d -> d.dayOfWeek() == day).map(PlannedWorkout::templateName).findFirst();
    }
}
