package com.vegas.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** POST /api/users/me/email/verification/confirm — код из письма */
public record ConfirmEmailRequest(
        @NotBlank(message = "введите код")
        @Pattern(regexp = "\\d{6}", message = "код — 6 цифр")
        String code
) {
}
