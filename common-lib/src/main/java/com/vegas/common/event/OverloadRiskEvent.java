package com.vegas.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Высокий риск перегрузки по упражнению; reasons — причины простыми словами. */
public record OverloadRiskEvent(
        UUID userId,
        UUID exerciseId,
        String exerciseName,
        String level,
        List<String> reasons,
        Instant detectedAt
) {
}
