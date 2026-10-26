package com.vegas.workout.controller;

import com.vegas.workout.dto.WeeklyPlanRequest;
import com.vegas.workout.dto.WeeklyPlanResponse;
import com.vegas.workout.security.CurrentUser;
import com.vegas.workout.service.WeeklyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
@Tag(name = "Weekly plan", description = "План на неделю: комплексы по дням")
public class WeeklyPlanController {

    private final WeeklyPlanService planService;

    @GetMapping
    @Operation(summary = "План на неделю: 7 дней с понедельника, template = null — отдых")
    public WeeklyPlanResponse get(@AuthenticationPrincipal Jwt jwt) {
        return planService.get(CurrentUser.id(jwt));
    }

    @PutMapping
    @Operation(summary = "Заменить план целиком")
    public WeeklyPlanResponse replace(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WeeklyPlanRequest request) {
        return planService.replace(CurrentUser.id(jwt), request);
    }
}
