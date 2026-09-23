package io.github.smaykell.aulavirtual.course.unit;

import io.github.smaykell.aulavirtual.common.storage.FileStorage;
import io.github.smaykell.aulavirtual.common.storage.StoredObject;
import io.github.smaykell.aulavirtual.course.exception.FileMaterialWithoutKeyException;
import io.github.smaykell.aulavirtual.course.exception.FileNotUploadedException;
import io.github.smaykell.aulavirtual.course.exception.FileTooLargeException;
import io.github.smaykell.aulavirtual.course.exception.FileTypeNotAllowedException;
import io.github.smaykell.aulavirtual.course.exception.ForeignFileException;
import io.github.smaykell.aulavirtual.course.exception.LinkMaterialWithoutUrlException;
import io.github.smaykell.aulavirtual.course.exception.MaterialNotFoundException;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialData;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialResponse;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialUploadRequest;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialUploadResponse;
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
    private final FileStorage fileStorage;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MaterialUploadResponse prepareUpload(String actorUsername, UUID unitId,
            MaterialUploadRequest request) {

        Unit unit = unitService.writable(actorUsername, unitId);
        requireAcceptedFile(request.type(), request.contentType(), request.size());

        String key = MaterialFiles.keyFor(unit.getCourseId(), request.fileName());
        return MaterialUploadResponse.from(key,
                fileStorage.presignUpload(key, request.contentType(), request.size()));
    }

    @Transactional
    public MaterialResponse create(String actorUsername, UUID unitId, MaterialData data) {
        Unit unit = unitService.writable(actorUsername, unitId);
        requireMatchingSource(data);
        claimNewFile(unit, null, data);

        return MaterialResponse.from(materialRepository.save(
                Material.create(unit.getId(), data, publicationOf(data))));
    }

    @Transactional
    public MaterialResponse update(String actorUsername, UUID materialId, MaterialData data) {
        Material material = existing(materialId);
        Unit unit = unitService.writable(actorUsername, material.getUnitId());
        requireMatchingSource(data);
        claimNewFile(unit, material.getStorageKey(), data);

        material.update(data, publicationOf(data));
        return MaterialResponse.from(material);
    }

    @Transactional
    public void delete(String actorUsername, UUID materialId) {
        Material material = existing(materialId);
        unitService.writable(actorUsername, material.getUnitId());
        materialRepository.delete(material);
    }

    private Material existing(UUID materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new MaterialNotFoundException(materialId));
    }

    private Instant publicationOf(MaterialData data) {
        return data.publishedAt() == null ? clock.instant() : data.publishedAt();
    }

    private void claimNewFile(Unit unit, String currentKey, MaterialData data) {
        if (data.type().isLink() || data.storageKey().trim().equals(currentKey)) {
            return;
        }
        String key = data.storageKey().trim();
        requireOwnFile(unit, key);

        StoredObject stored = fileStorage.describe(key)
                .orElseThrow(FileNotUploadedException::new);
        requireAcceptedFile(data.type(), stored.contentType(), stored.size());
        fileStorage.claim(key);
    }

    private void requireOwnFile(Unit unit, String key) {
        if (!MaterialFiles.belongsTo(key, unit.getCourseId())
                || materialRepository.existsByStorageKey(key)) {
            throw new ForeignFileException();
        }
    }

    private static void requireAcceptedFile(MaterialType type, String contentType, long size) {
        if (!type.accepts(contentType)) {
            throw new FileTypeNotAllowedException(type, contentType);
        }
        if (!type.fits(size)) {
            throw new FileTooLargeException(type);
        }
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
