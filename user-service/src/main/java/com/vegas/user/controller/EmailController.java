package com.vegas.user.controller;

import com.vegas.user.dto.ChangeEmailRequest;
import com.vegas.user.dto.ConfirmEmailRequest;
import com.vegas.user.dto.UserResponse;
import com.vegas.user.security.AuthenticatedUser;
import com.vegas.user.service.EmailCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Мой email: подтвердить кодом из письма, отправить код ещё раз, исправить адрес */
@RestController
@RequestMapping("/api/users/me/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Подтверждение и смена email")
@SecurityRequirement(name = "bearerAuth")
public class EmailController {

    private final EmailCodeService emailCodeService;

    @PostMapping("/verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отправить код ещё раз (не чаще раза в минуту)")
    public void resend(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        emailCodeService.resendVerification(currentUser.id());
    }

    @PostMapping("/verification/confirm")
    @Operation(summary = "Подтвердить email кодом из письма")
    public UserResponse confirm(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                @Valid @RequestBody ConfirmEmailRequest request) {
        return emailCodeService.confirmEmail(currentUser.id(), request.code());
    }

    @PutMapping
    @Operation(summary = "Исправить email (нужен пароль); на новый адрес сразу уходит код")
    public UserResponse change(@AuthenticationPrincipal AuthenticatedUser currentUser,
                               @Valid @RequestBody ChangeEmailRequest request) {
        return emailCodeService.changeEmail(currentUser.id(), request.email(), request.password());
    }
}
