package com.vegas.user.controller;

import com.vegas.user.dto.ForgotPasswordRequest;
import com.vegas.user.dto.ResetPasswordRequest;
import com.vegas.user.service.EmailCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** "Забыли пароль?" — без входа (под /api/auth): код на подтверждённый email и новый пароль */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
@Tag(name = "Password", description = "Восстановление пароля")
public class PasswordController {

    private final EmailCodeService emailCodeService;

    @PostMapping("/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Прислать код (ответ одинаковый, есть такой email или нет)")
    public void forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        emailCodeService.requestPasswordReset(request.email());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Новый пароль по коду из письма; все входы на других устройствах закрываются")
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        emailCodeService.resetPassword(request.email(), request.code(), request.newPassword());
    }
}
