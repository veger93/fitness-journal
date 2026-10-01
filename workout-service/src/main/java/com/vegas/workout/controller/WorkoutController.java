package com.vegas.workout.controller;

import com.vegas.workout.dto.AddWorkoutExerciseRequest;
import com.vegas.workout.dto.PageResponse;
import com.vegas.workout.dto.SetRequest;
import com.vegas.workout.dto.SetResponse;
import com.vegas.workout.dto.StartWorkoutRequest;
import com.vegas.workout.dto.WorkoutResponse;
import com.vegas.workout.dto.WorkoutSummaryResponse;
import com.vegas.workout.security.CurrentUser;
import com.vegas.workout.service.WorkoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.UUID;

/**
 * Тренировки. Вложенные URL отражают иерархию:
 * /api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}
 */
@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
@Tag(name = "Workouts", description = "Тренировки и подходы")
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Начать тренировку: {\"templateId\": \"...\"} или {} для пустой")
    public WorkoutResponse start(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartWorkoutRequest request) {
        return workoutService.start(CurrentUser.id(jwt), request);
    }

    /**
     * ResponseEntity — когда статус зависит от результата:
     * есть идущая тренировка -> 200 с телом, нет -> 204 без тела.
     */
    @GetMapping("/current")
    @Operation(summary = "Идущая тренировка (204, если нет)")
    public ResponseEntity<WorkoutResponse> current(@AuthenticationPrincipal Jwt jwt) {
        return workoutService.getCurrent(CurrentUser.id(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** @Min/@Max на параметрах Spring 6.1+ проверяет сам: page=-1 -> 400. */
    @GetMapping
    @Operation(summary = "История завершённых тренировок, новые сверху")
    public PageResponse<WorkoutSummaryResponse> history(@AuthenticationPrincipal Jwt jwt,
                                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return workoutService.history(CurrentUser.id(jwt), page, size);
    }

    @GetMapping("/{workoutId}")
    @Operation(summary = "Тренировка целиком")
    public WorkoutResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID workoutId) {
        return workoutService.getById(CurrentUser.id(jwt), workoutId);
    }

    @PostMapping("/{workoutId}/complete")
    @Operation(summary = "Завершить тренировку")
    public WorkoutResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID workoutId) {
        return workoutService.complete(CurrentUser.id(jwt), workoutId);
    }

    @DeleteMapping("/{workoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить тренировку (отменить идущую или убрать из истории)")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID workoutId) {
        workoutService.delete(CurrentUser.id(jwt), workoutId);
    }

    // ---------- упражнения ----------

    @PostMapping("/{workoutId}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Добавить упражнение в идущую тренировку")
    public WorkoutResponse addExercise(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable UUID workoutId,
                                       @Valid @RequestBody AddWorkoutExerciseRequest request) {
        return workoutService.addExercise(CurrentUser.id(jwt), workoutId, request);
    }

    @DeleteMapping("/{workoutId}/exercises/{workoutExerciseId}")
    @Operation(summary = "Убрать упражнение из идущей тренировки")
    public WorkoutResponse removeExercise(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID workoutId,
                                          @PathVariable UUID workoutExerciseId) {
        return workoutService.removeExercise(CurrentUser.id(jwt), workoutId, workoutExerciseId);
    }

    // ---------- подходы ----------

    @PostMapping("/{workoutId}/exercises/{workoutExerciseId}/sets")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Записать подход")
    public SetResponse addSet(@AuthenticationPrincipal Jwt jwt,
                              @PathVariable UUID workoutId,
                              @PathVariable UUID workoutExerciseId,
                              @Valid @RequestBody SetRequest request) {
        return workoutService.addSet(CurrentUser.id(jwt), workoutId, workoutExerciseId, request);
    }

    @PutMapping("/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}")
    @Operation(summary = "Исправить подход")
    public SetResponse updateSet(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable UUID workoutId,
                                 @PathVariable UUID workoutExerciseId,
                                 @PathVariable UUID setId,
                                 @Valid @RequestBody SetRequest request) {
        return workoutService.updateSet(CurrentUser.id(jwt), workoutId, workoutExerciseId, setId, request);
    }

    @DeleteMapping("/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить подход")
    public void deleteSet(@AuthenticationPrincipal Jwt jwt,
                          @PathVariable UUID workoutId,
                          @PathVariable UUID workoutExerciseId,
                          @PathVariable UUID setId) {
        workoutService.deleteSet(CurrentUser.id(jwt), workoutId, workoutExerciseId, setId);
    }
}
