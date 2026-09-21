package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.GradeData;
import io.github.smaykell.aulavirtual.assignment.dto.GradeResponse;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionData;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionResponse;
import io.github.smaykell.aulavirtual.assignment.exception.DeadlinePassedException;
import io.github.smaykell.aulavirtual.assignment.exception.EmptySubmissionException;
import io.github.smaykell.aulavirtual.assignment.exception.OnlyStudentsSubmitException;
import io.github.smaykell.aulavirtual.assignment.exception.ScoreOutOfRangeException;
import io.github.smaykell.aulavirtual.assignment.exception.SubmissionAlreadyGradedException;
import io.github.smaykell.aulavirtual.assignment.exception.SubmissionNotFoundException;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
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
    private final Clock clock;

    @Transactional
    public SubmissionResponse submit(String actorUsername, UUID assignmentId,
            SubmissionData data) {

        Assignment assignment = assignmentService.existing(assignmentId);
        UUID studentId = studentSubmitting(actorUsername, assignment);
        requireSomethingToHandIn(data);

        Instant now = clock.instant();
        if (!assignment.acceptsAt(now)) {
            throw new DeadlinePassedException();
        }

        return responseFor(submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(submission -> handInAgain(submission, data, assignment, now))
                .orElseGet(() -> submissionRepository.save(Submission.of(assignmentId, studentId,
                        data, now, statusAt(assignment, now)))));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> list(String actorUsername, UUID assignmentId,
            SubmissionStatus status, Pageable pageable) {

        Assignment assignment = assignmentService.existing(assignmentId);
        CourseMember member = assignmentService.memberFor(actorUsername, assignment);
        Page<Submission> submissions = submissionsFor(member, assignmentId, status, pageable);

        Map<UUID, StudentSummary> students = studentService.summariesOf(submissions.getContent()
                .stream().map(Submission::getStudentId).toList());
        Map<UUID, GradeResponse> grades = gradeService.bySource(GradeSource.ASSIGNMENT,
                submissions.getContent().stream().map(Submission::getId).toList());

        return PageResponse.of(submissions, submission -> SubmissionResponse.from(submission,
                students.get(submission.getStudentId()), grades.get(submission.getId())));
    }

    @Transactional
    public SubmissionResponse grade(String actorUsername, UUID submissionId, GradeData data) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        Assignment assignment = assignmentService.writable(actorUsername,
                submission.getAssignmentId());
        if (!assignment.accepts(data.score())) {
            throw new ScoreOutOfRangeException(assignment.getMaxScore());
        }

        Grade grade = gradeService.record(actorUsername, GradeSource.ASSIGNMENT,
                submission.getId(), submission.getStudentId(),
                assignmentService.courseOf(assignment), data.score(), data.feedback());
        submission.markGraded();

        return SubmissionResponse.from(submission,
                studentService.summaryOf(submission.getStudentId()), GradeResponse.from(grade));
    }

    private Submission handInAgain(Submission submission, SubmissionData data,
            Assignment assignment, Instant moment) {

        if (submission.isGraded()) {
            throw new SubmissionAlreadyGradedException();
        }
        submission.replace(data, moment, statusAt(assignment, moment));
        return submission;
    }

    private UUID studentSubmitting(String actorUsername, Assignment assignment) {
        CourseMember member = assignmentService.memberFor(actorUsername, assignment);
        if (member.studentId() == null) {
            throw new OnlyStudentsSubmitException();
        }
        return member.studentId();
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

    private SubmissionResponse responseFor(Submission submission) {
        return SubmissionResponse.from(submission,
                studentService.summaryOf(submission.getStudentId()),
                gradeService.bySource(GradeSource.ASSIGNMENT, List.of(submission.getId()))
                        .get(submission.getId()));
    }

    private static SubmissionStatus statusAt(Assignment assignment, Instant moment) {
        return assignment.isLate(moment) ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED;
    }

    private static void requireSomethingToHandIn(SubmissionData data) {
        if (isBlank(data.storageKey()) && isBlank(data.text())) {
            throw new EmptySubmissionException();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
