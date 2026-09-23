package io.github.smaykell.aulavirtual.common.storage;

import java.time.Instant;
import java.util.Map;

public record PresignedUpload(String url, Map<String, String> headers, Instant expiresAt) {
}
