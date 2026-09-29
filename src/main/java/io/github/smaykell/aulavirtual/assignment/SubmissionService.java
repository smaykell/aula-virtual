package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentFiles;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentOwner;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentService;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentResponse;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentUploadRequest;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentUploadResponse;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionData;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionResponse;
import io.github.smaykell.aulavirtual.assignment.exception.DeadlinePassedException;
import io.github.smaykell.aulavirtual.assignment.exception.EmptySubmissionException;
import io.github.smaykell.aulavirtual.assignment.exception.OnlyStudentsSubmitException;
import io.github.smaykell.aulavirtual.assignment.exception.SubmissionAlreadyGradedException;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentService assignmentService;
    private final GradeService gradeService;
    private final StudentService studentService;
    private final AttachmentService attachmentService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AttachmentUploadResponse prepareUpload(String actorUsername, UUID assignmentId,
            AttachmentUploadRequest request) {

        Assignment assignment = assignmentService.existing(assignmentId);
        CourseMember student = studentSubmitting(actorUsername, assignment);
        requireOpenAt(assignment, clock.instant());
        return attachmentService.prepareUpload(prefixFor(assignment, student), request);
    }

    @Transactional
    public SubmissionResponse submit(String actorUsername, UUID assignmentId,
            SubmissionData data) {

        Assignment assignment = assignmentService.existing(assignmentId);
        CourseMember student = studentSubmitting(actorUsername, assignment);
        UUID studentId = student.studentId();
        if (data.isEmpty()) {
            throw new EmptySubmissionException();
        }
        Instant now = clock.instant();
        requireOpenAt(assignment, now);

        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(previous -> handInAgain(previous, data, assignment, now))
                .orElseGet(() -> submissionRepository.save(Submission.of(assignmentId, studentId,
                        data, now, statusAt(assignment, now))));
        List<AttachmentResponse> attachments = attachmentService.replace(
                AttachmentOwner.ofSubmission(submission.getId()),
                prefixFor(assignment, student), data.attachmentsOrNone());
        return responseFor(student, submission, attachments);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> list(String actorUsername, UUID assignmentId,
            SubmissionStatus status, Pageable pageable) {

        Assignment assignment = assignmentService.existing(assignmentId);
        CourseMember member = assignmentService.memberFor(actorUsername, assignment);
        Page<Submission> submissions = submissionsFor(member, assignmentId, status, pageable);

        List<UUID> studentIds = submissions.getContent().stream()
                .map(Submission::getStudentId)
                .toList();
        Map<UUID, StudentSummary> students = studentService.summariesOf(studentIds);
        Map<UUID, GradeResponse> grades = gradeService.visibleTo(member,
                GradeSource.ASSIGNMENT, assignmentId, studentIds);
        Map<UUID, List<AttachmentResponse>> attachments = attachmentService.ofSubmissions(
                submissions.getContent().stream().map(Submission::getId).toList());

        return PageResponse.of(submissions, submission -> SubmissionResponse.from(submission,
                students.get(submission.getStudentId()),
                attachments.getOrDefault(submission.getId(), List.of()),
                grades.get(submission.getStudentId())));
    }

    private Submission handInAgain(Submission submission, SubmissionData data,
            Assignment assignment, Instant moment) {

        if (submission.isGraded()) {
            throw new SubmissionAlreadyGradedException();
        }
        submission.replace(data, moment, statusAt(assignment, moment));
        return submission;
    }

    private CourseMember studentSubmitting(String actorUsername, Assignment assignment) {
        CourseMember member = assignmentService.memberFor(actorUsername, assignment);
        if (member.studentId() == null) {
            throw new OnlyStudentsSubmitException();
        }
        return member;
    }

    private Page<Submission> submissionsFor(CourseMember member, UUID assignmentId,
            SubmissionStatus status, Pageable pageable) {

        if (!member.staff()) {
            return submissionRepository.findByAssignmentIdAndStudentId(assignmentId,
                    member.studentId(), pageable);
        }
        return status == null
                ? submissionRepository.findByAssignmentId(assignmentId, pageable)
                : submissionRepository.findByAssignmentIdAndStatus(assignmentId, status, pageable);
    }

    private SubmissionResponse responseFor(CourseMember member, Submission submission,
            List<AttachmentResponse> attachments) {

        UUID studentId = submission.getStudentId();
        return SubmissionResponse.from(submission, studentService.summaryOf(studentId),
                attachments, gradeService.visibleTo(member, GradeSource.ASSIGNMENT,
                        submission.getAssignmentId(), List.of(studentId)).get(studentId));
    }

    private static String prefixFor(Assignment assignment, CourseMember student) {
        return AttachmentFiles.submissionPrefix(assignment.getCourseId(), student.studentId());
    }

    private static void requireOpenAt(Assignment assignment, Instant moment) {
        if (!assignment.acceptsAt(moment)) {
            throw new DeadlinePassedException();
        }
    }

    private static SubmissionStatus statusAt(Assignment assignment, Instant moment) {
        return assignment.isLate(moment) ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED;
    }
}
