package com.vegas.workout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Комплекс упражнений (шаблон тренировки).
 * ownerId == null -> готовый (системный), иначе — из "Моих комплексов".
 */
@Entity
@Table(name = "workout_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(length = 30)
    private String icon;

    /**
     * @ManyToMany: одно упражнение может быть во многих комплексах, в комплексе — много упражнений.
     * Связь хранится в таблице workout_template_exercises.
     * @OrderColumn: Hibernate сам пишет индекс элемента списка в колонку position
     * и при загрузке восстанавливает порядок. Порядок упражнений в комплексе важен.
     */
    @ManyToMany
    @JoinTable(
            name = "workout_template_exercises",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "exercise_id")
    )
    @OrderColumn(name = "position")
    private List<Exercise> exercises = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static WorkoutTemplate custom(UUID ownerId, String name, String color, String icon, List<Exercise> exercises) {
        WorkoutTemplate template = new WorkoutTemplate();
        template.ownerId = ownerId;
        template.update(name, color, icon, exercises);
        return template;
    }

    /**
     * Список не заменяем новым объектом (this.exercises = exercises), а очищаем и наполняем:
     * Hibernate следит именно за своей коллекцией; подмена ссылки ломает отслеживание изменений.
     */
    public void update(String name, String color, String icon, List<Exercise> exercises) {
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.exercises.clear();
        this.exercises.addAll(exercises);
    }

    /** Копия в "Мои комплексы" — например, готового комплекса, чтобы подправить под себя. */
    public WorkoutTemplate copyFor(UUID userId) {
        return custom(userId, name, color, icon, new ArrayList<>(exercises));
    }

    public boolean isCustom() {
        return ownerId != null;
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public boolean isVisibleTo(UUID userId) {
        return !isCustom() || isOwnedBy(userId);
    }
}
