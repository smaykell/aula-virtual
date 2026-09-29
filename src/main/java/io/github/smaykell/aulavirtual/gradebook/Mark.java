package io.github.smaykell.aulavirtual.gradebook;

import java.math.BigDecimal;
import java.util.UUID;

record Mark(UUID categoryId, BigDecimal score, BigDecimal maxScore) {

    boolean belongsTo(UUID category) {
        return category.equals(categoryId);
    }
}
