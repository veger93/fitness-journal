package com.vegas.workout.entity;

import com.vegas.common.model.MuscleGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @Embeddable — не отдельная сущность со своим id, а "значение" внутри Exercise.
 * Хранится в таблице exercise_muscles (см. @ElementCollection в Exercise).
 * equals/hashCode обязательны: элементы лежат в Set, и Hibernate сравнивает их по значению.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseMuscle {

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle", nullable = false, length = 30)
    private MuscleGroup muscle;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private MuscleRole role;
}
