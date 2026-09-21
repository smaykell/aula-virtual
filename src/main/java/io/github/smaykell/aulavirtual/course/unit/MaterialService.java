package io.github.smaykell.aulavirtual.course.unit;

import io.github.smaykell.aulavirtual.course.exception.FileMaterialWithoutKeyException;
import io.github.smaykell.aulavirtual.course.exception.LinkMaterialWithoutUrlException;
import io.github.smaykell.aulavirtual.course.exception.MaterialNotFoundException;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialData;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final UnitService unitService;
    private final Clock clock;

    @Transactional
    public MaterialResponse create(String actorUsername, UUID unitId, MaterialData data) {
        Unit unit = unitService.writable(actorUsername, unitId);
        requireMatchingSource(data);

        return MaterialResponse.from(materialRepository.save(
                Material.create(unit.getId(), data, publicationOf(data))));
    }

    @Transactional
    public MaterialResponse update(String actorUsername, UUID materialId, MaterialData data) {
        Material material = writable(actorUsername, materialId);
        requireMatchingSource(data);

        material.update(data, publicationOf(data));
        return MaterialResponse.from(material);
    }

    @Transactional
    public void delete(String actorUsername, UUID materialId) {
        materialRepository.delete(writable(actorUsername, materialId));
    }

    private Material writable(String actorUsername, UUID materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new MaterialNotFoundException(materialId));
        unitService.writable(actorUsername, material.getUnitId());
        return material;
    }

    private Instant publicationOf(MaterialData data) {
        return data.publishedAt() == null ? clock.instant() : data.publishedAt();
    }

    private static void requireMatchingSource(MaterialData data) {
        if (data.type().isLink() && isBlank(data.externalUrl())) {
            throw new LinkMaterialWithoutUrlException();
        }
        if (!data.type().isLink() && isBlank(data.storageKey())) {
            throw new FileMaterialWithoutKeyException(data.type());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
