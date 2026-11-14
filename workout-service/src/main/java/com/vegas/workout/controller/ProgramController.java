package com.vegas.workout.controller;

import com.vegas.workout.dto.ExclusionResponse;
import com.vegas.workout.dto.ProgramAlternativeResponse;
import com.vegas.workout.dto.ProgramRequest;
import com.vegas.workout.dto.ProgramResponse;
import com.vegas.workout.dto.ProgramSwapRequest;
import com.vegas.workout.security.CurrentUser;
import com.vegas.workout.service.ProgramService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@Tag(name = "Programs", description = "Программа тренировок по анкете (docs/program.md)")
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Собрать программу по анкете: комплексы дней + план недели. Прежняя уходит в архив")
    public ProgramResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProgramRequest request) {
        return programService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping("/current")
    @Operation(summary = "Текущая программа; 204 — программы нет")
    public ResponseEntity<ProgramResponse> current(@AuthenticationPrincipal Jwt jwt) {
        return programService.getCurrent(CurrentUser.id(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отказаться от программы: в архив, комплексы и план остаются")
    public void abandon(@AuthenticationPrincipal Jwt jwt) {
        programService.abandon(CurrentUser.id(jwt));
    }

    @GetMapping("/current/slots/{slotId}/alternatives")
    @Operation(summary = "Чем можно заменить упражнение на этом месте (то же семейство)")
    public List<ProgramAlternativeResponse> alternatives(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID slotId) {
        return programService.alternatives(CurrentUser.id(jwt), slotId);
    }

    @PostMapping("/current/slots/{slotId}/swap")
    @Operation(summary = "Заменить упражнение — один раз на место; excludeOld = «больше не предлагать»")
    public ProgramResponse swap(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID slotId,
                                @Valid @RequestBody ProgramSwapRequest request) {
        return programService.swap(CurrentUser.id(jwt), slotId, request);
    }

    @GetMapping("/exclusions")
    @Operation(summary = "Упражнения «больше не предлагать»")
    public List<ExclusionResponse> exclusions(@AuthenticationPrincipal Jwt jwt) {
        return programService.exclusions(CurrentUser.id(jwt));
    }

    @DeleteMapping("/exclusions/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Вернуть упражнение в подбор")
    public void removeExclusion(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID exerciseId) {
        programService.removeExclusion(CurrentUser.id(jwt), exerciseId);
    }
}
