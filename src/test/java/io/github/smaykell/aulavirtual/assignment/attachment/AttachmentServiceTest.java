package io.github.smaykell.aulavirtual.assignment.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.assignment.dto.AttachmentData;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentResponse;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentUploadRequest;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.common.storage.FileCleanup;
import io.github.smaykell.aulavirtual.common.storage.FileStorage;
import io.github.smaykell.aulavirtual.common.storage.PresignedUpload;
import io.github.smaykell.aulavirtual.common.storage.StoredObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID ASSIGNMENT = UUID.randomUUID();
    private static final AttachmentOwner OWNER = AttachmentOwner.ofAssignment(ASSIGNMENT);
    private static final String PREFIX = AttachmentFiles.assignmentPrefix(COURSE);
    private static final String PDF = "application/pdf";
    private static final StoredObject SMALL_PDF = new StoredObject(2048, PDF);

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private FileCleanup fileCleanup;

    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(attachmentRepository, fileStorage,
                fileCleanup);
    }

    @Test
    void the_upload_key_is_generated_under_the_prefix_of_its_owner() {
        when(fileStorage.presignUpload(any(), any(), anyLong())).thenReturn(
                new PresignedUpload("https://s3/put", Map.of(), Instant.EPOCH));

        String key = attachmentService.prepareUpload(PREFIX,
                new AttachmentUploadRequest("Guía de práctica.pdf", PDF, 2048L)).storageKey();

        assertThat(key).startsWith(PREFIX).endsWith("/Guia-de-practica.pdf");
    }

    @Test
    void a_type_that_is_not_accepted_is_not_even_signed() {
        ApiException error = assertThrows(ApiException.class,
                () -> attachmentService.prepareUpload(PREFIX,
                        new AttachmentUploadRequest("virus.exe", "application/x-msdownload",
                                2048L)));

        assertThat(error.getCode()).isEqualTo("ASG_FILE_TYPE_NOT_ALLOWED");
        verify(fileStorage, never()).presignUpload(any(), any(), anyLong());
    }

    @Test
    void a_file_over_fifty_megabytes_is_rejected() {
        ApiException error = assertThrows(ApiException.class,
                () -> attachmentService.prepareUpload(PREFIX,
                        new AttachmentUploadRequest("tesis.pdf", PDF, 51L * 1024 * 1024)));

        assertThat(error.getCode()).isEqualTo("ASG_FILE_TOO_LARGE");
    }

    @Test
    void a_new_file_is_checked_claimed_and_stored_with_its_type_and_size() {
        String key = PREFIX + UUID.randomUUID() + "/guia.pdf";
        givenNothingAttached();
        when(fileStorage.describe(key)).thenReturn(Optional.of(SMALL_PDF));
        List<Attachment> saved = givenAttachmentsAreStored();

        List<AttachmentResponse> attachments = attachmentService.replace(OWNER, PREFIX,
                List.of(file(key, "Guía de práctica.pdf")));

        verify(fileStorage).claim(key);
        assertThat(saved).singleElement().satisfies(attachment -> {
            assertThat(attachment.getAssignmentId()).isEqualTo(ASSIGNMENT);
            assertThat(attachment.getContentType()).isEqualTo(PDF);
            assertThat(attachment.getSizeBytes()).isEqualTo(2048L);
        });
        assertThat(attachments).singleElement()
                .satisfies(each -> assertThat(each.title()).isEqualTo("Guía de práctica.pdf"));
    }

    @Test
    void a_file_uploaded_for_another_course_or_person_is_foreign() {
        givenNothingAttached();
        String someoneElses = AttachmentFiles.assignmentPrefix(UUID.randomUUID()) + "x/guia.pdf";

        ApiException error = assertThrows(ApiException.class,
                () -> attachmentService.replace(OWNER, PREFIX, List.of(file(someoneElses, null))));

        assertThat(error.getCode()).isEqualTo("ASG_FOREIGN_FILE");
        verify(fileStorage, never()).claim(any());
    }

    @Test
    void a_file_already_used_by_another_attachment_is_foreign() {
        givenNothingAttached();
        String key = PREFIX + "x/guia.pdf";
        when(attachmentRepository.existsByStorageKey(key)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> attachmentService.replace(OWNER, PREFIX, List.of(file(key, null))));

        assertThat(error.getCode()).isEqualTo("ASG_FOREIGN_FILE");
    }

    @Test
    void a_file_that_never_reached_the_storage_is_not_attached() {
        givenNothingAttached();
        String key = PREFIX + "x/guia.pdf";
        when(fileStorage.describe(key)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> attachmentService.replace(OWNER, PREFIX, List.of(file(key, null))));

        assertThat(error.getCode()).isEqualTo("ASG_FILE_NOT_UPLOADED");
    }

    @Test
    void keeping_a_file_only_renames_and_moves_it_while_the_rest_is_dropped_after_commit() {
        Attachment kept = stored(Attachment.file(OWNER, "guia.pdf", PREFIX + "a/guia.pdf",
                SMALL_PDF, 2));
        Attachment dropped = stored(Attachment.file(OWNER, "viejo.pdf", PREFIX + "b/viejo.pdf",
                SMALL_PDF, 1));
        when(attachmentRepository.findByAssignmentIdOrderByPosition(ASSIGNMENT))
                .thenReturn(List.of(dropped, kept));

        attachmentService.replace(OWNER, PREFIX, List.of(file(kept.getStorageKey(), "Guía")));

        assertThat(kept.getTitle()).isEqualTo("Guía");
        assertThat(kept.getPosition()).isEqualTo(1);
        verify(fileStorage, never()).claim(any());
        verify(attachmentRepository).deleteAll(List.of(dropped));
        verify(fileCleanup).deleteAfterCommit(dropped.getStorageKey());
    }

    @Test
    void a_link_takes_its_address_as_title_when_it_has_none() {
        givenNothingAttached();
        List<Attachment> saved = givenAttachmentsAreStored();

        attachmentService.replace(OWNER, PREFIX, List.of(new AttachmentData(AttachmentKind.LINK,
                " ", null, "https://youtu.be/rcp")));

        assertThat(saved).singleElement().satisfies(link -> {
            assertThat(link.getTitle()).isEqualTo("https://youtu.be/rcp");
            assertThat(link.file()).isEmpty();
        });
    }

    @Test
    void a_file_attachment_without_its_file_is_rejected() {
        givenNothingAttached();

        ApiException error = assertThrows(ApiException.class,
                () -> attachmentService.replace(OWNER, PREFIX,
                        List.of(new AttachmentData(AttachmentKind.FILE, "Guía", null, null))));

        assertThat(error.getCode()).isEqualTo("ASG_ATTACHMENT_WITHOUT_SOURCE");
    }

    private void givenNothingAttached() {
        when(attachmentRepository.findByAssignmentIdOrderByPosition(ASSIGNMENT))
                .thenReturn(List.of());
    }

    private List<Attachment> givenAttachmentsAreStored() {
        List<Attachment> saved = new ArrayList<>();
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(call -> {
            Attachment attachment = stored(call.getArgument(0));
            saved.add(attachment);
            return attachment;
        });
        return saved;
    }

    private static Attachment stored(Attachment attachment) {
        ReflectionTestUtils.setField(attachment, "id", UUID.randomUUID());
        return attachment;
    }

    private static AttachmentData file(String key, String title) {
        return new AttachmentData(AttachmentKind.FILE, title, key, null);
    }
}
