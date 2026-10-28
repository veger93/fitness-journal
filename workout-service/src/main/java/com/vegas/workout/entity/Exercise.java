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
import org.hibernate.annotations.Formula;
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

    /**
     * Ключ картинки упражнения (файлы на фронте: /exercises/<ключ>-1.svg и -2.svg — начало и конец движения).
     * @Formula — поле не хранится в таблице exercises: Hibernate сам подставляет подзапрос в SELECT.
     * Только для чтения; картинки добавляются миграциями в exercise_media.
     */
    @Formula("(select m.storage_key from exercise_media m"
            + " where m.exercise_id = id and m.media_type = 'IMAGE' and m.gender is null)")
    private String imageKey;

    /** Своё упражнение "удалено", но по нему есть история: из каталога пропадает, тренировки и графики остаются */
    @Column(name = "archived_at")
    private Instant archivedAt;

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

    public void archive(Instant now) {
        this.archivedAt = now;
    }

    /** Пользователь снова создаёт упражнение с тем же названием — возвращаем старое вместе с историей */
    public void restore() {
        this.archivedAt = null;
    }

    public boolean isArchived() {
        return archivedAt != null;
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
