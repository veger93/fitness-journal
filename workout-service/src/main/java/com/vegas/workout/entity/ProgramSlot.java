package com.vegas.workout.entity;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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

import java.util.Objects;
import java.util.UUID;

/**
 * «Место» в программе: какое упражнение стоит в дне программы на позиции position и как его делать.
 * Главное правило (П14): заменить упражнение можно ОДИН раз и только на упражнение того же семейства движения —
 * иначе программа превращается в кашу, а прогресс не видно (docs/program.md). Правило живёт здесь, в сущности:
 * его нельзя обойти, вызвав другой сервис или забыв проверку в контроллере.
 */
@Entity
@Table(name = "program_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** День программы — обычный комплекс */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkoutTemplate template;

    @Column(nullable = false)
    private int position;

    /** Семейство движения на этом месте — фиксируется при создании и не меняется при замене */
    @Column(name = "movement_pattern", nullable = false, length = 30)
    private String movementPattern;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    /** Что стояло до замены; null — не меняли */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_exercise_id")
    private Exercise originalExercise;

    @Embedded
    private SetScheme scheme;

    @Column(nullable = false)
    private boolean swapped;

    ProgramSlot(Program program, WorkoutTemplate template, int position, Exercise exercise, SetScheme scheme) {
        if (exercise.getMovementPattern() == null) {
            // без семейства не понять, на что можно заменить, — генератор такие упражнения не берёт
            throw new BadRequestException("У упражнения «" + exercise.getName() + "» нет семейства движения");
        }
        this.program = program;
        this.template = template;
        this.position = position;
        this.movementPattern = exercise.getMovementPattern();
        this.exercise = exercise;
        this.scheme = scheme;
    }

    /**
     * Замена упражнения — сразу в программе («только сегодня» нет), один раз, внутри семейства.
     * Комплекс дня (WorkoutTemplate) сервис обновляет сам — сущность места о его списке упражнений не знает.
     */
    public void swapTo(Exercise replacement) {
        if (swapped) {
            throw new BusinessException("Это упражнение уже заменяли — заменить можно один раз");
        }
        if (Objects.equals(replacement.getId(), exercise.getId())) {
            throw new BadRequestException("Это то же самое упражнение");
        }
        if (!movementPattern.equals(replacement.getMovementPattern())) {
            throw new BadRequestException("Заменить можно только на упражнение с той же работой мышц");
        }
        if (replacement.isArchived()) {
            throw new BadRequestException("Этого упражнения больше нет в каталоге");
        }
        originalExercise = exercise;
        exercise = replacement;
        swapped = true;
    }
}
