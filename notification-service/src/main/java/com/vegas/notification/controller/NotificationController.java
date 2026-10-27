package com.vegas.notification.controller;

import com.vegas.notification.dto.NotificationResponse;
import com.vegas.notification.dto.NotificationSettingsRequest;
import com.vegas.notification.dto.NotificationSettingsResponse;
import com.vegas.notification.dto.PageResponse;
import com.vegas.notification.dto.UnreadCountResponse;
import com.vegas.notification.security.CurrentUser;
import com.vegas.notification.service.NotificationHistoryService;
import com.vegas.notification.service.NotificationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSettingsService settingsService;
    private final NotificationHistoryService historyService;

    @Operation(summary = "История уведомлений, новые сверху")
    @GetMapping
    public PageResponse<NotificationResponse> history(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam(defaultValue = "0") @Min(0) int page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return historyService.history(CurrentUser.id(jwt), page, size);
    }

    @Operation(summary = "Сколько непрочитанных — для колокольчика в шапке")
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return historyService.unreadCount(CurrentUser.id(jwt));
    }

    /**
     * POST, а не GET: запрос меняет данные. 204 No Content — отвечать нечем, всё прошло.
     */
    @Operation(summary = "Отметить все уведомления прочитанными (пользователь открыл ленту)")
    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
        historyService.markAllRead(CurrentUser.id(jwt));
    }

    @Operation(summary = "Настройки напоминаний")
    @GetMapping("/settings")
    public NotificationSettingsResponse settings(@AuthenticationPrincipal Jwt jwt) {
        return settingsService.get(CurrentUser.id(jwt));
    }

    @Operation(summary = "Изменить настройки напоминаний (целиком)")
    @PutMapping("/settings")
    public NotificationSettingsResponse updateSettings(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody NotificationSettingsRequest request) {
        return settingsService.update(CurrentUser.id(jwt), request);
    }
}
