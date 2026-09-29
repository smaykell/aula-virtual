package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.common.storage.PresignedDownload;
import java.time.Instant;

public record AttachmentDownloadResponse(String url, Instant expiresAt) {

    public static AttachmentDownloadResponse from(PresignedDownload download) {
        return new AttachmentDownloadResponse(download.url(), download.expiresAt());
    }
}
