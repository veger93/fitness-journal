package com.vegas.workout.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Страница результатов в простом стабильном формате.
 * Сам Page из Spring Data в JSON не отдаём: его структура — внутренняя деталь Spring
 * и может меняться между версиями.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
