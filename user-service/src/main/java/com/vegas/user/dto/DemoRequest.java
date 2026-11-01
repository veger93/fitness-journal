package com.vegas.user.dto;

import com.vegas.user.service.DemoScenario;
import jakarta.validation.constraints.NotNull;

/** POST /api/auth/demo — "Посмотреть изнутри" на лендинге: как спортсмен или как тренер (docs/demo.md) */
public record DemoRequest(
        @NotNull(message = "выберите: спортсмен или тренер")
        DemoScenario scenario
) {
}
