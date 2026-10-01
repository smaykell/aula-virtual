package io.github.smaykell.aulavirtual.exam.dto;

import io.github.smaykell.aulavirtual.exam.AttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttemptResponse(
        UUID id,
        UUID examId,
        UUID studentId,
        int number,
        AttemptStatus status,
        Instant startedAt,
        Instant deadline,
        Instant submittedAt,
        BigDecimal score,
        List<AttemptQuestionResponse> questions) {
}
