package com.vegas.workout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * Техника выполнения упражнения: исходное положение, шаги, частые ошибки.
 * Связь с Exercise 1:1 через общий ключ (exercise_id). Отдельная сущность, а не поля в Exercise:
 * технику показываем только в карточке, а каталог грузится целиком — незачем тащить тексты в каждый список.
 * Только для чтения: тексты добавляются миграциями.
 */
@Entity
@Table(name = "exercise_technique")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseTechnique {

    @Id
    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(nullable = false)
    private String setup;

    /** text[] в PostgreSQL. SqlTypes.ARRAY — Hibernate 6 сам переводит его в List<String> и обратно */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> steps;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> mistakes;
}
