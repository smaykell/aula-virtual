package io.github.smaykell.aulavirtual.exam;

import java.math.BigDecimal;

public record Grading(BigDecimal score, boolean pendingReview) {
}
