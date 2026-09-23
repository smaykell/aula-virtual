package io.github.smaykell.aulavirtual.course.unit;

import java.util.Locale;
import java.util.Set;

public enum MaterialType {

    PDF(megabytes(50), "application/pdf"),
    VIDEO(megabytes(500), "video/mp4", "video/webm"),
    PPT(megabytes(50), "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    DOC(megabytes(50), "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    LINK(0);

    private final long maxBytes;
    private final Set<String> contentTypes;

    MaterialType(long maxBytes, String... contentTypes) {
        this.maxBytes = maxBytes;
        this.contentTypes = Set.of(contentTypes);
    }

    public boolean isLink() {
        return this == LINK;
    }

    public boolean accepts(String contentType) {
        return contentType != null && contentTypes.contains(mediaTypeOf(contentType));
    }

    public boolean fits(long size) {
        return size > 0 && size <= maxBytes;
    }

    public long maxMegabytes() {
        return maxBytes / megabytes(1);
    }

    public boolean opensInBrowser() {
        return this == PDF || this == VIDEO;
    }

    private static String mediaTypeOf(String contentType) {
        int parameters = contentType.indexOf(';');
        String mediaType = parameters < 0 ? contentType : contentType.substring(0, parameters);
        return mediaType.strip().toLowerCase(Locale.ROOT);
    }

    private static long megabytes(int amount) {
        return amount * 1024L * 1024L;
    }
}
