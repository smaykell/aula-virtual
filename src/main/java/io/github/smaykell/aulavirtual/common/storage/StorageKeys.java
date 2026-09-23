package io.github.smaykell.aulavirtual.common.storage;

import java.text.Normalizer;
import java.util.Locale;

public final class StorageKeys {

    private static final int BASE_NAME_MAX = 90;
    private static final int EXTENSION_MAX = 10;
    private static final String FALLBACK_NAME = "archivo";
    private static final char SEPARATOR = '/';
    private static final char WINDOWS_SEPARATOR = '\\';

    private StorageKeys() {
    }

    public static String safeFileName(String fileName) {
        String ascii = Normalizer.normalize(lastSegmentOf(fileName).strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        int dot = ascii.lastIndexOf('.');
        String baseName = dot > 0 ? ascii.substring(0, dot) : ascii;
        String extension = dot > 0 ? ascii.substring(dot + 1) : "";
        return withExtension(
                truncated(slug(baseName), BASE_NAME_MAX),
                truncated(slug(extension).toLowerCase(Locale.ROOT), EXTENSION_MAX));
    }

    public static String fileNameOf(String key) {
        return key.substring(key.lastIndexOf(SEPARATOR) + 1);
    }

    private static String lastSegmentOf(String path) {
        return path.substring(
                Math.max(path.lastIndexOf(SEPARATOR), path.lastIndexOf(WINDOWS_SEPARATOR)) + 1);
    }

    private static String withExtension(String baseName, String extension) {
        String name = baseName.isEmpty() ? FALLBACK_NAME : baseName;
        return extension.isEmpty() ? name : name + "." + extension;
    }

    private static String slug(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
    }

    private static String truncated(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
