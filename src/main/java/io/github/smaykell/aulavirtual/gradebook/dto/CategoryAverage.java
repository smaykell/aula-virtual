package io.github.smaykell.aulavirtual.gradebook.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryAverage(UUID categoryId, BigDecimal score) {
}
