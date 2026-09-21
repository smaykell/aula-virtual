package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.modules.course.dto.MaterialData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "materials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Material extends BaseEntity {

    @Column(name = "unit_id", nullable = false, updatable = false)
    private UUID unitId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MaterialType type;

    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "external_url", length = 2048)
    private String externalUrl;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    private Material(UUID unitId, MaterialData data, Instant publishedAt) {
        this.unitId = unitId;
        update(data, publishedAt);
    }

    public static Material create(UUID unitId, MaterialData data, Instant publishedAt) {
        return new Material(unitId, data, publishedAt);
    }

    public final void update(MaterialData data, Instant publishedAt) {
        this.title = data.title().trim();
        this.type = data.type();
        this.storageKey = data.type().isLink() ? null : data.storageKey().trim();
        this.externalUrl = data.type().isLink() ? data.externalUrl().trim() : null;
        this.publishedAt = publishedAt;
        this.visible = data.visible();
    }
}
