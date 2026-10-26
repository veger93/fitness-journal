package com.vegas.workout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.UUID;

/**
 * День плана на неделю: в этот день недели — этот комплекс (см. миграцию 010).
 * Суррогатный id вместо составного ключа (user_id, day_of_week): так проще сущность,
 * а уникальность "один комплекс на день" держит UNIQUE в БД.
 */
@Entity
@Table(name = "weekly_plan_days")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyPlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** java.time.DayOfWeek как строка: MONDAY ... SUNDAY */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkoutTemplate template;

    public WeeklyPlanDay(UUID userId, DayOfWeek dayOfWeek, WorkoutTemplate template) {
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.template = template;
    }
}
