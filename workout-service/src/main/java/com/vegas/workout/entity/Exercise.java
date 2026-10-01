package com.vegas.workout.entity;

import com.vegas.common.model.BodyPart;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Упражнение каталога.
 * ownerId == null -> системное (видят все, менять нельзя);
 * ownerId != null -> своё упражнение пользователя (видит и меняет только владелец).
 */
@Entity
@Table(name = "exercises")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_part", nullable = false, length = 20)
    private BodyPart bodyPart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_type", nullable = false, length = 20)
    private TrackingType trackingType;

    @Column(length = 30)
    private String icon;

    @Column(length = 1000)
    private String note;

    /**
     * Коллекция "значений" в отдельной таблице.
     * LAZY: мышцы грузятся только когда нужны. Чтобы в списке не было N+1 запросов,
     * в репозитории подгружаем их одним запросом через @EntityGraph.
     */
    @ElementCollection
    @CollectionTable(name = "exercise_muscles", joinColumns = @JoinColumn(name = "exercise_id"))
    private Set<ExerciseMuscle> muscles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Своё упражнение пользователя (экран "Своё упражнение"). Мышцы пользователь не выбирает. */
    public static Exercise custom(UUID ownerId, String name, BodyPart bodyPart, Equipment equipment,
                                  TrackingType trackingType, String icon, String note) {
        Exercise exercise = new Exercise();
        exercise.ownerId = ownerId;
        exercise.update(name, bodyPart, equipment, trackingType, icon, note);
        return exercise;
    }

    /** Один метод вместо шести сеттеров: поля меняются только вместе и только осмысленно. */
    public void update(String name, BodyPart bodyPart, Equipment equipment,
                       TrackingType trackingType, String icon, String note) {
        this.name = name;
        this.bodyPart = bodyPart;
        this.equipment = equipment;
        this.trackingType = trackingType;
        this.icon = icon;
        this.note = note;
    }

    public boolean isCustom() {
        return ownerId != null;
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    /** Системное видят все, своё — только владелец. */
    public boolean isVisibleTo(UUID userId) {
        return !isCustom() || isOwnedBy(userId);
    }
}
