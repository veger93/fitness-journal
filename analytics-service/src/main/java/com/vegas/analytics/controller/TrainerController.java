package com.vegas.analytics.controller;

import com.vegas.analytics.dto.ClientSignalResponse;
import com.vegas.analytics.security.CurrentUser;
import com.vegas.analytics.service.TrainerSignalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Сводки тренера по клиентам. Запрос — от имени самого тренера (без X-Client-Id):
 * у обычного пользователя клиентов нет — вернётся пустой список.
 */
@RestController
@RequestMapping("/api/analytics/trainer")
@RequiredArgsConstructor
@Tag(name = "Trainer", description = "Сводки тренера по клиентам")
public class TrainerController {

    private final TrainerSignalsService trainerSignalsService;

    @GetMapping("/clients")
    @Operation(summary = "Главный сигнал по каждому клиенту: перегрузка, плато, давно не был, рекорд")
    public List<ClientSignalResponse> clientSignals(@AuthenticationPrincipal Jwt jwt) {
        return trainerSignalsService.clientSignals(CurrentUser.id(jwt));
    }
}
