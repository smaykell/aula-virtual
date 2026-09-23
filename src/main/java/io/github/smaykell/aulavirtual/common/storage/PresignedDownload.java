package io.github.smaykell.aulavirtual.common.storage;

import java.time.Instant;

public record PresignedDownload(String url, Instant expiresAt) {
}
