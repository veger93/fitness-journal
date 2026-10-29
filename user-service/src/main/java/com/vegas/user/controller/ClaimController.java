package com.vegas.user.controller;

import com.vegas.user.dto.AuthResponse;
import com.vegas.user.dto.ClaimPreviewResponse;
import com.vegas.user.dto.ClaimRequest;
import com.vegas.user.security.RefreshTokenCookie;
import com.vegas.user.service.AuthTokens;
import com.vegas.user.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Передача аккаунта клиенту по ссылке. Под /api/auth: без входа (у клиента ещё нет пароля),
 * пускает сам токен из ссылки. Получил аккаунт — сразу вошёл: refresh-токен в cookie, как при регистрации.
 */
@RestController
@RequestMapping("/api/auth/claim")
@RequiredArgsConstructor
@Tag(name = "Claim", description = "Клиент получает аккаунт от тренера")
public class ClaimController {

    private final ClaimService claimService;
    private final RefreshTokenCookie refreshTokenCookie;

    @GetMapping("/{token}")
    @Operation(summary = "Что передают: имя клиента и тренера (404 — ссылка недействительна)")
    public ClaimPreviewResponse preview(@PathVariable String token) {
        return claimService.preview(token);
    }

    @PostMapping("/{token}")
    @Operation(summary = "Принять аккаунт: email, пароль, остаться с тренером или нет")
    public ResponseEntity<AuthResponse> claim(@PathVariable String token, @Valid @RequestBody ClaimRequest request) {
        AuthTokens tokens = claimService.claim(token, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(tokens.refreshToken()).toString())
                .body(tokens.response());
    }
}
