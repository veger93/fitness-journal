package com.vegas.user.controller;

import com.vegas.user.dto.DeleteAccountRequest;
import com.vegas.user.dto.UserResponse;
import com.vegas.user.security.AuthenticatedUser;
import com.vegas.user.security.RefreshTokenCookie;
import com.vegas.user.service.AccountDeletionService;
import com.vegas.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Эндпоинты текущего пользователя. Требуют JWT.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Текущий пользователь")
@SecurityRequirement(name = "bearerAuth") // в Swagger у этих методов появится замочек
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletionService;
    private final RefreshTokenCookie refreshTokenCookie;

    /**
     * id берём из токена, а НЕ из URL: /api/users/me нельзя подменить на чужой id.
     * @AuthenticationPrincipal достаёт то, что JwtAuthenticationFilter положил в SecurityContext.
     */
    @GetMapping("/me")
    @Operation(summary = "Данные текущего пользователя")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getById(currentUser.id());
    }

    /**
     * Удалить свой аккаунт. POST, а не DELETE: нужен пароль в теле, а у тела DELETE по HTTP-спецификации
     * нет смысла (RFC 9110) — прокси и библиотеки вправе его выбросить.
     * Ответ заодно велит браузеру удалить cookie с refresh-токеном (в базе токены уже удалены каскадом).
     */
    @PostMapping("/me/deletion")
    @Operation(summary = "Удалить свой аккаунт и все данные (нужен пароль)")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                         @Valid @RequestBody DeleteAccountRequest request) {
        accountDeletionService.deleteAccount(currentUser.id(), request.password());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
                .build();
    }
}
