package com.vegas.user.controller;

import com.vegas.user.dto.AuthResponse;
import com.vegas.user.dto.DemoRequest;
import com.vegas.user.dto.LoginRequest;
import com.vegas.user.dto.RegisterRequest;
import com.vegas.user.security.ClientIp;
import com.vegas.user.security.RefreshTokenCookie;
import com.vegas.user.service.AuthService;
import com.vegas.user.service.AuthTokens;
import com.vegas.user.service.DemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Публичные эндпоинты (токен не нужен — см. SecurityConfig).
 *
 * Две части "пропуска":
 * - access-токен (15 минут) — в теле ответа; фронт держит его в памяти и шлёт в Authorization: Bearer;
 * - refresh-токен (30 дней) — в httpOnly-cookie; по нему фронт молча получает новый access-токен.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Регистрация, вход, обновление токена, выход")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;
    private final DemoService demoService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация по email и паролю")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withCookie(HttpStatus.CREATED, authService.register(request)); // 201: создан пользователь
    }

    /**
     * "Посмотреть изнутри" на лендинге (docs/demo.md): временный пользователь с заполненным дневником, сразу вход.
     * 201 — создан пользователь, как при регистрации. Тренировки догенерируются за несколько секунд.
     */
    @PostMapping("/demo")
    @Operation(summary = "Демо: войти как спортсмен или тренер с готовыми данными (живёт сутки)")
    public ResponseEntity<AuthResponse> demo(@Valid @RequestBody DemoRequest request, HttpServletRequest http) {
        return withCookie(HttpStatus.CREATED, demoService.start(request.scenario(), ClientIp.of(http)));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход по email и паролю")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withCookie(HttpStatus.OK, authService.login(request));
    }

    /**
     * Новый access-токен по refresh-cookie (и новый refresh — старый одноразовый).
     * Cookie нет или он недействителен -> 401 и команда браузеру удалить cookie (см. GlobalExceptionHandler).
     */
    @PostMapping("/refresh")
    @Operation(summary = "Обновить access-токен по refresh-cookie")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        return withCookie(HttpStatus.OK, authService.refresh(refreshToken));
    }

    /** Выход: отзываем refresh-токен и удаляем cookie. Access-токен доживёт свои минуты — фронт его просто забудет */
    @PostMapping("/logout")
    @Operation(summary = "Выход")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withCookie(HttpStatus status, AuthTokens tokens) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(tokens.refreshToken()).toString())
                .body(tokens.response());
    }
}
