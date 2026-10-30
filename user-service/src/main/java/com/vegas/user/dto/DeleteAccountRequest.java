package com.vegas.user.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /api/users/me/deletion — пароль: чужой с открытого телефона не удалит аккаунт */
public record DeleteAccountRequest(
        @NotBlank(message = "введите пароль")
        String password
) {
}
