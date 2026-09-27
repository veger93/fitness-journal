package com.vegas.user.controller;

import com.vegas.user.dto.UserResponse;
import com.vegas.user.security.AuthenticatedUser;
import com.vegas.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    /**
     * id берём из токена, а НЕ из URL: /api/users/me нельзя подменить на чужой id.
     * @AuthenticationPrincipal достаёт то, что JwtAuthenticationFilter положил в SecurityContext.
     */
    @GetMapping("/me")
    @Operation(summary = "Данные текущего пользователя")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.getById(currentUser.id());
    }
}
