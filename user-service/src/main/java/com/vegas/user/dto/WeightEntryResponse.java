package com.vegas.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WeightEntryResponse(
        UUID id,
        BigDecimal weightKg,
        LocalDate measuredOn
) {
}
