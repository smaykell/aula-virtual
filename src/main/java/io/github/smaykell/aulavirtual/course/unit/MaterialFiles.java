package io.github.smaykell.aulavirtual.course.unit;

import io.github.smaykell.aulavirtual.common.storage.StorageKeys;
import java.util.UUID;

final class MaterialFiles {

    private MaterialFiles() {
    }

    static String keyFor(UUID courseId, String fileName) {
        return prefixOf(courseId) + UUID.randomUUID() + "/" + StorageKeys.safeFileName(fileName);
    }

    static boolean belongsTo(String key, UUID courseId) {
        return key.startsWith(prefixOf(courseId));
    }

    private static String prefixOf(UUID courseId) {
        return "courses/" + courseId + "/materials/";
    }
}
