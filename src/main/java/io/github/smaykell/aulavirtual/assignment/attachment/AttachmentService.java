package io.github.smaykell.aulavirtual.assignment.attachment;

import io.github.smaykell.aulavirtual.assignment.dto.AttachmentData;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentResponse;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentUploadRequest;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentUploadResponse;
import io.github.smaykell.aulavirtual.assignment.exception.AttachmentWithoutSourceException;
import io.github.smaykell.aulavirtual.assignment.exception.FileNotUploadedException;
import io.github.smaykell.aulavirtual.assignment.exception.FileTooLargeException;
import io.github.smaykell.aulavirtual.assignment.exception.FileTypeNotAllowedException;
import io.github.smaykell.aulavirtual.assignment.exception.ForeignFileException;
import io.github.smaykell.aulavirtual.common.storage.FileCleanup;
import io.github.smaykell.aulavirtual.common.storage.FileStorage;
import io.github.smaykell.aulavirtual.common.storage.StorageKeys;
import io.github.smaykell.aulavirtual.common.storage.StoredObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorage fileStorage;
    private final FileCleanup fileCleanup;

    public AttachmentUploadResponse prepareUpload(String keyPrefix,
            AttachmentUploadRequest request) {

        requireAcceptedFile(request.contentType(), request.size());
        String key = AttachmentFiles.keyUnder(keyPrefix, request.fileName());
        return AttachmentUploadResponse.from(key,
                fileStorage.presignUpload(key, request.contentType(), request.size()));
    }

    public List<AttachmentResponse> replace(AttachmentOwner owner, String keyPrefix,
            List<AttachmentData> wanted) {

        List<Attachment> current = currentOf(owner);
        Map<String, Attachment> currentFiles = new HashMap<>(current.stream()
                .filter(attachment -> attachment.file().isPresent())
                .collect(Collectors.toMap(Attachment::getStorageKey, Function.identity())));

        List<Attachment> placed = new ArrayList<>();
        for (int index = 0; index < wanted.size(); index++) {
            placed.add(place(owner, keyPrefix, wanted.get(index), index + 1, currentFiles));
        }

        drop(current.stream().filter(each -> !placed.contains(each)).toList());
        return placed.stream().map(AttachmentResponse::from).toList();
    }

    public void deleteAll(AttachmentOwner owner) {
        drop(currentOf(owner));
    }

    public List<AttachmentResponse> of(AttachmentOwner owner) {
        return currentOf(owner).stream().map(AttachmentResponse::from).toList();
    }

    public Map<UUID, List<AttachmentResponse>> ofAssignments(Collection<UUID> assignmentIds) {
        return groupedBy(attachmentRepository.findByAssignmentIdInOrderByPosition(assignmentIds),
                Attachment::getAssignmentId);
    }

    public Map<UUID, List<AttachmentResponse>> ofSubmissions(Collection<UUID> submissionIds) {
        return groupedBy(attachmentRepository.findBySubmissionIdInOrderByPosition(submissionIds),
                Attachment::getSubmissionId);
    }

    private void drop(List<Attachment> attachments) {
        attachmentRepository.deleteAll(attachments);
        attachments.forEach(each -> each.file().ifPresent(fileCleanup::deleteAfterCommit));
    }

    private List<Attachment> currentOf(AttachmentOwner owner) {
        return owner.assignmentId() != null
                ? attachmentRepository.findByAssignmentIdOrderByPosition(owner.assignmentId())
                : attachmentRepository.findBySubmissionIdOrderByPosition(owner.submissionId());
    }

    private Attachment place(AttachmentOwner owner, String keyPrefix, AttachmentData data,
            int position, Map<String, Attachment> currentFiles) {

        return switch (data.kind()) {
            case FILE -> placeFile(owner, keyPrefix, data, position, currentFiles);
            case LINK -> placeLink(owner, data, position);
        };
    }

    private Attachment placeFile(AttachmentOwner owner, String keyPrefix, AttachmentData data,
            int position, Map<String, Attachment> currentFiles) {

        if (isBlank(data.storageKey())) {
            throw new AttachmentWithoutSourceException();
        }
        String key = data.storageKey().trim();
        String title = titleOr(data, StorageKeys.fileNameOf(key));
        Attachment kept = currentFiles.remove(key);
        if (kept != null) {
            kept.place(title, position);
            return kept;
        }

        requireOwnFile(keyPrefix, key);
        StoredObject stored = fileStorage.describe(key).orElseThrow(FileNotUploadedException::new);
        requireAcceptedFile(stored.contentType(), stored.size());
        fileStorage.claim(key);
        return attachmentRepository.save(Attachment.file(owner, title, key, stored, position));
    }

    private Attachment placeLink(AttachmentOwner owner, AttachmentData data, int position) {
        if (isBlank(data.externalUrl())) {
            throw new AttachmentWithoutSourceException();
        }
        String url = data.externalUrl().trim();
        return attachmentRepository.save(
                Attachment.link(owner, titleOr(data, url), url, position));
    }

    private void requireOwnFile(String keyPrefix, String key) {
        if (!key.startsWith(keyPrefix) || attachmentRepository.existsByStorageKey(key)) {
            throw new ForeignFileException();
        }
    }

    private static void requireAcceptedFile(String contentType, long size) {
        if (!AttachmentFiles.accepts(contentType)) {
            throw new FileTypeNotAllowedException(contentType);
        }
        if (!AttachmentFiles.fits(size)) {
            throw new FileTooLargeException(AttachmentFiles.MAX_MEGABYTES);
        }
    }

    private static Map<UUID, List<AttachmentResponse>> groupedBy(List<Attachment> attachments,
            Function<Attachment, UUID> owner) {

        return attachments.stream().collect(Collectors.groupingBy(owner,
                Collectors.mapping(AttachmentResponse::from, Collectors.toList())));
    }

    private static String titleOr(AttachmentData data, String fallback) {
        return isBlank(data.title()) ? fallback : data.title().trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
