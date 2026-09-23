package io.github.smaykell.aulavirtual.course.unit.dto;

import io.github.smaykell.aulavirtual.common.storage.PresignedUpload;
import java.time.Instant;
import java.util.Map;

public record MaterialUploadResponse(
        String storageKey,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Instant expiresAt) {

    private static final String UPLOAD_METHOD = "PUT";

    public static MaterialUploadResponse from(String storageKey, PresignedUpload upload) {
        return new MaterialUploadResponse(storageKey, upload.url(), UPLOAD_METHOD,
                upload.headers(), upload.expiresAt());
    }
}
