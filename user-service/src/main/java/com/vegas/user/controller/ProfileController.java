package com.vegas.user.controller;

import com.vegas.user.dto.ProfileResponse;
import com.vegas.user.dto.RecordWeightRequest;
import com.vegas.user.dto.UpdateProfileRequest;
import com.vegas.user.dto.WeightEntryResponse;
import com.vegas.user.security.AuthenticatedUser;
import com.vegas.user.service.BodyWeightService;
import com.vegas.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Профиль и вес ТЕКУЩЕГО пользователя. Везде /me: id берётся из токена.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Физ. данные и история веса")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;
    private final BodyWeightService bodyWeightService;

    @GetMapping("/profile")
    @Operation(summary = "Профиль (onboardingCompleted=false -> показать онбординг)")
    public ProfileResponse getProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return profileService.getProfile(currentUser.id());
    }

    @PutMapping("/profile")
    @Operation(summary = "Заполнить/обновить физ. данные (онбординг, настройки)")
    public ProfileResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(currentUser.id(), request);
    }

    @GetMapping("/weight")
    @Operation(summary = "История веса, новые записи сверху")
    public List<WeightEntryResponse> weightHistory(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return bodyWeightService.history(currentUser.id());
    }

    /**
     * PUT, а не POST: запрос ИДЕМПОТЕНТНЫЙ. Сколько раз ни отправь
     * "вес за 2026-09-27 = 80.5", результат один — одна запись за этот день.
     * Дату присылает фронт: так "сегодня" считается в часовом поясе пользователя, а не сервера.
     */
    @PutMapping("/weight/{date}")
    @Operation(summary = "Записать вес за день (формат даты: 2026-09-27)")
    public WeightEntryResponse recordWeight(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @Valid @RequestBody RecordWeightRequest request) {
        return bodyWeightService.record(currentUser.id(), date, request);
    }
}
