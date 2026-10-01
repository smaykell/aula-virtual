package io.github.smaykell.aulavirtual.exam.dto;

import java.util.UUID;

public record AttemptOptionResponse(UUID id, String text, Boolean correct) {
}
