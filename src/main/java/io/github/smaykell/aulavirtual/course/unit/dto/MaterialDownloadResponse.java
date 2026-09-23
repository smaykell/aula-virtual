package io.github.smaykell.aulavirtual.course.unit.dto;

import io.github.smaykell.aulavirtual.common.storage.PresignedDownload;
import java.time.Instant;

public record MaterialDownloadResponse(String url, Instant expiresAt) {

    public static MaterialDownloadResponse from(PresignedDownload download) {
        return new MaterialDownloadResponse(download.url(), download.expiresAt());
    }
}
