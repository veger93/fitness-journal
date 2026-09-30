package com.vegas.workout.controller;

import com.vegas.workout.dto.WorkoutTemplateRequest;
import com.vegas.workout.dto.WorkoutTemplateResponse;
import com.vegas.workout.security.CurrentUser;
import com.vegas.workout.service.WorkoutTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Комплексы упражнений")
public class WorkoutTemplateController {

    private final WorkoutTemplateService templateService;

    @GetMapping
    @Operation(summary = "Мои (custom=true) и готовые (custom=false) комплексы")
    public List<WorkoutTemplateResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return templateService.list(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Комплекс с упражнениями по порядку")
    public WorkoutTemplateResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return templateService.getById(CurrentUser.id(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать свой комплекс")
    public WorkoutTemplateResponse create(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody WorkoutTemplateRequest request) {
        return templateService.create(CurrentUser.id(jwt), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить свой комплекс (готовые — 403)")
    public WorkoutTemplateResponse update(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID id,
                                          @Valid @RequestBody WorkoutTemplateRequest request) {
        return templateService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204: удалено, в ответе ничего нет
    @Operation(summary = "Удалить свой комплекс")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        templateService.delete(CurrentUser.id(jwt), id);
    }

    /** Действие над ресурсом, которое не укладывается в CRUD, — отдельный под-путь с глаголом. */
    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Скопировать комплекс (например, готовый) в свои")
    public WorkoutTemplateResponse copy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return templateService.copy(CurrentUser.id(jwt), id);
    }
}
