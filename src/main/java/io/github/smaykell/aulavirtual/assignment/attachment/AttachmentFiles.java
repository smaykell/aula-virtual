package io.github.smaykell.aulavirtual.assignment.attachment;

import io.github.smaykell.aulavirtual.common.storage.StorageKeys;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class AttachmentFiles {

    public static final int MAX_PER_OWNER = 10;
    public static final long MAX_MEGABYTES = 50;

    private static final long MAX_BYTES = MAX_MEGABYTES * 1024 * 1024;
    private static final Set<String> INLINE_TYPES = Set.of("application/pdf", "image/jpeg",
            "image/png", "image/webp");
    private static final Set<String> ACCEPTED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "text/plain",
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/zip",
            "application/x-zip-compressed");

    private AttachmentFiles() {
    }

    public static String assignmentPrefix(UUID courseId) {
        return "courses/" + courseId + "/assignments/";
    }

    public static String submissionPrefix(UUID courseId, UUID studentId) {
        return "courses/" + courseId + "/submissions/" + studentId + "/";
    }

    public static String keyUnder(String prefix, String fileName) {
        return prefix + UUID.randomUUID() + "/" + StorageKeys.safeFileName(fileName);
    }

    public static boolean accepts(String contentType) {
        return contentType != null && ACCEPTED_TYPES.contains(mediaTypeOf(contentType));
    }

    public static boolean fits(long size) {
        return size > 0 && size <= MAX_BYTES;
    }

    public static boolean opensInBrowser(String contentType) {
        return contentType != null && INLINE_TYPES.contains(mediaTypeOf(contentType));
    }

    private static String mediaTypeOf(String contentType) {
        int parameters = contentType.indexOf(';');
        String mediaType = parameters < 0 ? contentType : contentType.substring(0, parameters);
        return mediaType.strip().toLowerCase(Locale.ROOT);
    }
}
