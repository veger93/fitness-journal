package com.vegas.workout.controller;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.dto.DeleteExerciseResponse;
import com.vegas.workout.dto.ExerciseRequest;
import com.vegas.workout.dto.ExerciseResponse;
import com.vegas.workout.dto.TechniqueResponse;
import com.vegas.workout.security.CurrentUser;
import com.vegas.workout.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Каталог упражнений. Удалить можно только своё; если по нему есть тренировки или комплексы,
 * оно не удаляется, а прячется из каталога (история остаётся).
 */
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises", description = "Каталог упражнений")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    @Operation(summary = "Каталог: системные + свои. Фильтры необязательны: ?bodyPart=CHEST&search=жим")
    public List<ExerciseResponse> search(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(required = false) BodyPart bodyPart,
                                         @RequestParam(required = false) String search) {
        return exerciseService.search(CurrentUser.id(jwt), bodyPart, search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Упражнение по id")
    public ExerciseResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exerciseService.getById(CurrentUser.id(jwt), id);
    }

    /** 200 — техника есть, 204 — для этого упражнения техники нет (например, своё упражнение) */
    @GetMapping("/{id}/technique")
    @Operation(summary = "Техника выполнения: исходное положение, шаги, частые ошибки")
    public ResponseEntity<TechniqueResponse> technique(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exerciseService.getTechnique(CurrentUser.id(jwt), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать своё упражнение")
    public ExerciseResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExerciseRequest request) {
        return exerciseService.create(CurrentUser.id(jwt), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить своё упражнение (системные — 403)")
    public ExerciseResponse update(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody ExerciseRequest request) {
        return exerciseService.update(CurrentUser.id(jwt), id, request);
    }

    /** Не 204, а тело с результатом: фронту важно, удалили совсем или только спрятали — от этого зависит текст */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить своё упражнение (с историей — спрятать из каталога)")
    public DeleteExerciseResponse delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exerciseService.delete(CurrentUser.id(jwt), id);
    }
}
