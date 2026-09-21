package io.github.smaykell.aulavirtual.modules.course.dto;

import io.github.smaykell.aulavirtual.modules.course.Material;
import io.github.smaykell.aulavirtual.modules.course.MaterialType;
import java.time.Instant;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        UUID unitId,
        String title,
        MaterialType type,
        String storageKey,
        String externalUrl,
        Instant publishedAt,
        boolean visible) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(material.getId(), material.getUnitId(), material.getTitle(),
                material.getType(), material.getStorageKey(), material.getExternalUrl(),
                material.getPublishedAt(), material.isVisible());
    }
}
