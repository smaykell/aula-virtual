package io.github.smaykell.aulavirtual.exam.dto;

import io.github.smaykell.aulavirtual.exam.AttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AttemptSummary(
        UUID id,
        UUID studentId,
        int number,
        AttemptStatus status,
        Instant startedAt,
        Instant deadline,
        Instant submittedAt,
        BigDecimal score) {
}
