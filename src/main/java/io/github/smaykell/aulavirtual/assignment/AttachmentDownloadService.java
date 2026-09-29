package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.attachment.Attachment;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentFiles;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentRepository;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentDownloadResponse;
import io.github.smaykell.aulavirtual.assignment.exception.AttachmentIsLinkException;
import io.github.smaykell.aulavirtual.assignment.exception.AttachmentNotFoundException;
import io.github.smaykell.aulavirtual.assignment.exception.SubmissionOutOfReachException;
import io.github.smaykell.aulavirtual.common.storage.FileStorage;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttachmentDownloadService {

    private final AttachmentRepository attachmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentService assignmentService;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public AttachmentDownloadResponse download(String actorUsername, UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
        requireReadable(actorUsername, attachment);
        String key = attachment.file().orElseThrow(AttachmentIsLinkException::new);
        return AttachmentDownloadResponse.from(
                fileStorage.presignDownload(key, dispositionOf(attachment)));
    }

    private void requireReadable(String actorUsername, Attachment attachment) {
        if (attachment.belongsToAnAssignment()) {
            assignmentService.memberFor(actorUsername,
                    assignmentService.existing(attachment.getAssignmentId()));
            return;
        }
        Submission submission = submissionRepository.findById(attachment.getSubmissionId())
                .orElseThrow(() -> new AttachmentNotFoundException(attachment.getId()));
        CourseMember member = assignmentService.memberFor(actorUsername,
                assignmentService.existing(submission.getAssignmentId()));
        if (!member.staff() && !submission.getStudentId().equals(member.studentId())) {
            throw new SubmissionOutOfReachException();
        }
    }

    private static ContentDisposition dispositionOf(Attachment attachment) {
        ContentDisposition.Builder disposition =
                AttachmentFiles.opensInBrowser(attachment.getContentType())
                        ? ContentDisposition.inline()
                        : ContentDisposition.attachment();
        return disposition.filename(attachment.getTitle(), StandardCharsets.UTF_8).build();
    }
}
