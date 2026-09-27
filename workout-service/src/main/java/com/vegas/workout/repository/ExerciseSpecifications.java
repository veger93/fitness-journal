package com.vegas.workout.repository;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.entity.Exercise;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

/**
 * Specification = кусочек WHERE, их можно склеивать через and()/or().
 * Если фильтр не задан — возвращаем null, и Spring Data просто пропускает это условие.
 */
public final class ExerciseSpecifications {

    private ExerciseSpecifications() {
    }

    /** WHERE owner_id IS NULL OR owner_id = :userId — системные + свои. */
    public static Specification<Exercise> visibleTo(UUID userId) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("ownerId")),
                cb.equal(root.get("ownerId"), userId)
        );
    }

    public static Specification<Exercise> hasBodyPart(BodyPart bodyPart) {
        return (root, query, cb) -> bodyPart == null ? null : cb.equal(root.get("bodyPart"), bodyPart);
    }

    /** Поиск по части названия без учёта регистра: lower(name) LIKE '%жим%'. */
    public static Specification<Exercise> nameContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }
}
