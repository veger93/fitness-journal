package com.vegas.analytics.controller;

import com.vegas.analytics.dto.DeloadPlanResponse;
import com.vegas.analytics.dto.ExerciseInsightsResponse;
import com.vegas.analytics.dto.ExerciseProgressResponse;
import com.vegas.analytics.dto.ProgressPeriod;
import com.vegas.analytics.security.CurrentUser;
import com.vegas.analytics.service.ExerciseInsightsService;
import com.vegas.analytics.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Прогресс по упражнениям")
public class AnalyticsController {

    private final ProgressService progressService;
    private final ExerciseInsightsService insightsService;

    @GetMapping("/exercises/{exerciseId}/progress")
    @Operation(summary = "График 1ПМ и метрики упражнения за период (M1, M3, M6, Y1, ALL)")
    public ExerciseProgressResponse progress(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID exerciseId,
                                             @RequestParam(defaultValue = "M3") ProgressPeriod period) {
        return progressService.progress(CurrentUser.id(jwt), exerciseId, period);
    }

    @GetMapping("/exercises/{exerciseId}/insights")
    @Operation(summary = "Плато, риск перегрузки и рекомендация системы")
    public ExerciseInsightsResponse insights(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID exerciseId) {
        return insightsService.insights(CurrentUser.id(jwt), exerciseId);
    }

    @GetMapping("/exercises/{exerciseId}/deload-plan")
    @Operation(summary = "Сгенерировать план разгрузочной недели")
    public DeloadPlanResponse deloadPlan(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID exerciseId) {
        return insightsService.deloadPlan(CurrentUser.id(jwt), exerciseId);
    }
}
