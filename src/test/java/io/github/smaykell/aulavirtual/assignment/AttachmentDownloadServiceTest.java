package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.assignment.attachment.Attachment;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentOwner;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentRepository;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.common.storage.FileStorage;
import io.github.smaykell.aulavirtual.common.storage.PresignedDownload;
import io.github.smaykell.aulavirtual.common.storage.StoredObject;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ContentDisposition;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AttachmentDownloadServiceTest {

    private static final UUID COURSE = AssignmentFixtures.COURSE;
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUIS = UUID.randomUUID();
    private static final StoredObject PDF = new StoredObject(2048, "application/pdf");
    private static final StoredObject ZIP = new StoredObject(2048, "application/zip");

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private FileStorage fileStorage;

    private AttachmentDownloadService downloadService;

    private final Assignment assignment = AssignmentFixtures.assignment();

    @BeforeEach
    void setUp() {
        downloadService = new AttachmentDownloadService(attachmentRepository,
                submissionRepository, assignmentService, fileStorage);
    }

    @Test
    void anyone_in_the_course_downloads_what_the_teacher_attached_and_a_pdf_opens_in_place() {
        Attachment guide = givenTheAttachment(Attachment.file(
                AttachmentOwner.ofAssignment(assignment.getId()), "Guía.pdf", "k/guia.pdf", PDF, 1));
        givenTheCourseSeesThemAs("ana", new CourseMember(COURSE, false, ANA));
        givenSignedDownloads();

        downloadService.download("ana", guide.getId());

        ArgumentCaptor<ContentDisposition> disposition =
                ArgumentCaptor.forClass(ContentDisposition.class);
        verify(fileStorage).presignDownload(eq("k/guia.pdf"), disposition.capture());
        assertThat(disposition.getValue().isInline()).isTrue();
    }

    @Test
    void the_student_downloads_its_own_submission_and_a_zip_comes_as_a_download() {
        Attachment handedIn = givenTheSubmissionAttachmentOf(ANA, ZIP);
        givenTheCourseSeesThemAs("ana", new CourseMember(COURSE, false, ANA));
        givenSignedDownloads();

        downloadService.download("ana", handedIn.getId());

        ArgumentCaptor<ContentDisposition> disposition =
                ArgumentCaptor.forClass(ContentDisposition.class);
        verify(fileStorage).presignDownload(any(), disposition.capture());
        assertThat(disposition.getValue().isAttachment()).isTrue();
    }

    @Test
    void a_student_does_not_download_the_submission_of_a_classmate() {
        Attachment handedIn = givenTheSubmissionAttachmentOf(LUIS, PDF);
        givenTheCourseSeesThemAs("ana", new CourseMember(COURSE, false, ANA));

        ApiException error = assertThrows(ApiException.class,
                () -> downloadService.download("ana", handedIn.getId()));

        assertThat(error.getCode()).isEqualTo("ASG_SUBMISSION_OUT_OF_REACH");
        verify(fileStorage, never()).presignDownload(any(), any());
    }

    @Test
    void the_teacher_downloads_the_submission_of_any_student() {
        Attachment handedIn = givenTheSubmissionAttachmentOf(LUIS, PDF);
        givenTheCourseSeesThemAs("juan", new CourseMember(COURSE, true, null));
        givenSignedDownloads();

        assertThat(downloadService.download("juan", handedIn.getId()).url())
                .isEqualTo("https://s3/get");
    }

    @Test
    void a_link_has_nothing_to_download() {
        Attachment link = givenTheAttachment(Attachment.link(
                AttachmentOwner.ofAssignment(assignment.getId()), "Video", "https://youtu.be/x", 1));
        givenTheCourseSeesThemAs("ana", new CourseMember(COURSE, false, ANA));

        ApiException error = assertThrows(ApiException.class,
                () -> downloadService.download("ana", link.getId()));

        assertThat(error.getCode()).isEqualTo("ASG_ATTACHMENT_IS_LINK");
    }

    private Attachment givenTheSubmissionAttachmentOf(UUID studentId, StoredObject stored) {
        Submission submission = AssignmentFixtures.submission(assignment.getId(), studentId,
                AssignmentFixtures.NOW, SubmissionStatus.SUBMITTED);
        when(submissionRepository.findById(submission.getId()))
                .thenReturn(Optional.of(submission));
        return givenTheAttachment(Attachment.file(AttachmentOwner.ofSubmission(submission.getId()),
                "entrega", "k/entrega", stored, 1));
    }

    private Attachment givenTheAttachment(Attachment attachment) {
        ReflectionTestUtils.setField(attachment, "id", UUID.randomUUID());
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        return attachment;
    }

    private void givenTheCourseSeesThemAs(String actorUsername, CourseMember member) {
        when(assignmentService.existing(assignment.getId())).thenReturn(assignment);
        when(assignmentService.memberFor(actorUsername, assignment)).thenReturn(member);
    }

    private void givenSignedDownloads() {
        when(fileStorage.presignDownload(any(), any()))
                .thenReturn(new PresignedDownload("https://s3/get", Instant.EPOCH));
    }
}
