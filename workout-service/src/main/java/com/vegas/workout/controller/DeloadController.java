package com.vegas.workout.controller;

import com.vegas.workout.dto.DeloadRequest;
import com.vegas.workout.dto.DeloadResponse;
import com.vegas.workout.security.CurrentUser;
import com.vegas.workout.service.DeloadService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Режим разгрузки. Разгрузка "принадлежит" упражнению, поэтому старт и стоп — под /api/exercises/{id}/deload.
 * PUT, а не POST: повторный запрос не создаёт вторую разгрузку, а заменяет текущую.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Deload", description = "Режим разгрузки по упражнению")
public class DeloadController {

    private final DeloadService deloadService;

    @GetMapping("/api/deloads")
    @Operation(summary = "Активные разгрузки пользователя")
    public List<DeloadResponse> active(@AuthenticationPrincipal Jwt jwt) {
        return deloadService.active(CurrentUser.id(jwt));
    }

    @PutMapping("/api/exercises/{exerciseId}/deload")
    @Operation(summary = "Начать разгрузку (заменяет текущую по этому упражнению)")
    public DeloadResponse start(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID exerciseId,
                                @Valid @RequestBody DeloadRequest request) {
        return deloadService.start(CurrentUser.id(jwt), exerciseId, request);
    }

    @DeleteMapping("/api/exercises/{exerciseId}/deload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Закончить разгрузку раньше срока")
    public void stop(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID exerciseId) {
        deloadService.stop(CurrentUser.id(jwt), exerciseId);
    }
}
