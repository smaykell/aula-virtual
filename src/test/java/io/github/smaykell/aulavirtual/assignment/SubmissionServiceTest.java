package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentFiles;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentKind;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentOwner;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentService;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentData;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionData;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionResponse;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.student.StudentService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    private static final Instant NOW = AssignmentFixtures.NOW;
    private static final Instant PAST_DEADLINE = NOW.minusSeconds(3600);
    private static final UUID UNIT = AssignmentFixtures.UNIT;
    private static final UUID COURSE = AssignmentFixtures.COURSE;
    private static final UUID STUDENT = UUID.randomUUID();
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private GradeService gradeService;

    @Mock
    private StudentService studentService;

    @Mock
    private AttachmentService attachmentService;

    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(submissionRepository, assignmentService,
                gradeService, studentService, attachmentService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void a_submission_of_only_attachments_goes_under_the_folder_of_its_student() {
        Assignment assignment = givenTheAssignmentFor("ana.estudiante", STUDENT);
        givenNoPreviousSubmission(assignment);
        givenTheSubmissionIsStored();
        givenTheStudentAndItsGrades();
        List<AttachmentData> attachments = List.of(new AttachmentData(AttachmentKind.LINK,
                "Mi video", null, "https://youtu.be/rcp"));

        submissionService.submit("ana.estudiante", assignment.getId(),
                new SubmissionData(null, attachments));

        verify(attachmentService).replace(any(AttachmentOwner.class),
                eq(AttachmentFiles.submissionPrefix(COURSE, STUDENT)), eq(attachments));
    }

    @Test
    void handing_in_before_the_deadline_leaves_the_submission_on_time() {
        Assignment assignment = givenTheAssignmentFor("ana.estudiante", STUDENT);
        givenNoPreviousSubmission(assignment);
        givenTheSubmissionIsStored();
        givenTheStudentAndItsGrades();

        SubmissionResponse submission = submissionService.submit("ana.estudiante",
                assignment.getId(), AssignmentFixtures.text("Mi respuesta"));

        assertThat(submission.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submission.submittedAt()).isEqualTo(NOW);
        assertThat(submission.text()).isEqualTo("Mi respuesta");
        assertThat(submission.student().lastName()).isEqualTo("Quispe Rojas");
    }

    @Test
    void handing_in_after_the_deadline_is_marked_late_when_the_task_admits_it() {
        Assignment assignment = givenTheAssignmentFor("ana.estudiante", STUDENT,
                PAST_DEADLINE, true);
        givenNoPreviousSubmission(assignment);
        givenTheSubmissionIsStored();
        givenTheStudentAndItsGrades();

        assertThat(submissionService.submit("ana.estudiante", assignment.getId(),
                AssignmentFixtures.text("Tarde pero llego")).status())
                .isEqualTo(SubmissionStatus.LATE);
    }

    @Test
    void handing_in_after_a_deadline_that_admits_nothing_is_rejected() {
        Assignment assignment = givenTheAssignmentFor("ana.estudiante", STUDENT,
                PAST_DEADLINE, false);

        ApiException error = assertThrows(ApiException.class,
                () -> submissionService.submit("ana.estudiante", assignment.getId(),
                        AssignmentFixtures.text("Tarde")));

        assertThat(error.getCode()).isEqualTo("ASG_DEADLINE_PASSED");
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void a_submission_without_file_and_without_text_is_rejected() {
        Assignment assignment = givenTheAssignmentFor("ana.estudiante", STUDENT);

        ApiException error = assertThrows(ApiException.class,
                () -> submissionService.submit("ana.estudiante", assignment.getId(),
                        new SubmissionData("   ", null)));

        assertThat(error.getCode()).isEqualTo("ASG_EMPTY_SUBMISSION");
    }

    @Test
    void a_teacher_does_not_hand_in_the_task_of_its_own_course() {
        Assignment assignment = AssignmentFixtures.assignment();
        when(assignmentService.existing(assignment.getId())).thenReturn(assignment);
        when(assignmentService.memberFor("juan", assignment))
                .thenReturn(new CourseMember(COURSE, true, null));

        ApiException error = assertThrows(ApiException.class,
                () -> submissionService.submit("juan", assignment.getId(),
                        AssignmentFixtures.text("No deberia")));

        assertThat(error.getCode()).isEqualTo("ASG_ONLY_STUDENTS_SUBMIT");
    }

    @Test
    void handing_in_again_replaces_the_same_submission() {
        Assignment assignment = givenTheAssignmentFor("ana.estudiante", STUDENT);
        Submission previous = givenThePreviousSubmission(assignment, SubmissionStatus.SUBMITTED);
        givenTheStudentAndItsGrades();

        SubmissionResponse submission = submissionService.submit("ana.estudiante",
                assignment.getId(), AssignmentFixtures.text("Version corregida"));

        assertThat(submission.id()).isEqualTo(previous.getId());
        assertThat(submission.text()).isEqualTo("Version corregida");
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void what_is_already_graded_does_not_admit_another_submission() {
        Assignment assignment = givenTheAssignmentFor("ana.estudiante", STUDENT);
        givenThePreviousSubmission(assignment, SubmissionStatus.GRADED);

        ApiException error = assertThrows(ApiException.class,
                () -> submissionService.submit("ana.estudiante", assignment.getId(),
                        AssignmentFixtures.text("Otra vez")));

        assertThat(error.getCode()).isEqualTo("ASG_ALREADY_GRADED");
    }

    @Test
    void the_teacher_reads_every_submission_and_the_student_only_its_own() {
        Assignment assignment = AssignmentFixtures.assignment();
        Submission submission = AssignmentFixtures.submission(assignment.getId(), STUDENT, NOW,
                SubmissionStatus.SUBMITTED);
        when(assignmentService.existing(assignment.getId())).thenReturn(assignment);
        when(assignmentService.memberFor("ana.estudiante", assignment))
                .thenReturn(new CourseMember(COURSE, false, STUDENT));
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), STUDENT,
                FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(submission), FIRST_PAGE, 1));
        when(studentService.summariesOf(List.of(STUDENT)))
                .thenReturn(Map.of(STUDENT, AssignmentFixtures.student(STUDENT)));
        when(gradeService.visibleTo(any(), eq(GradeSource.ASSIGNMENT), any(), any())).thenReturn(Map.of());

        PageResponse<SubmissionResponse> page = submissionService.list("ana.estudiante",
                assignment.getId(), null, FIRST_PAGE);

        assertThat(page.content()).singleElement()
                .satisfies(found -> assertThat(found.student().id()).isEqualTo(STUDENT));
        verify(submissionRepository, never())
                .findByAssignmentId(assignment.getId(), FIRST_PAGE);
    }

    private Assignment givenTheAssignmentFor(String actorUsername, UUID studentId) {
        return givenTheAssignmentFor(actorUsername, studentId, AssignmentFixtures.DUE_AT, false);
    }

    private Assignment givenTheAssignmentFor(String actorUsername, UUID studentId, Instant dueAt,
            boolean allowsLate) {

        Assignment assignment = AssignmentFixtures.assignment(dueAt, allowsLate);
        when(assignmentService.existing(assignment.getId())).thenReturn(assignment);
        when(assignmentService.memberFor(actorUsername, assignment))
                .thenReturn(new CourseMember(COURSE, false, studentId));
        return assignment;
    }

    private void givenNoPreviousSubmission(Assignment assignment) {
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), STUDENT))
                .thenReturn(Optional.empty());
    }

    private Submission givenThePreviousSubmission(Assignment assignment,
            SubmissionStatus status) {

        Submission submission = AssignmentFixtures.submission(assignment.getId(), STUDENT, NOW,
                status);
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), STUDENT))
                .thenReturn(Optional.of(submission));
        return submission;
    }

    private void givenTheStudentAndItsGrades() {
        when(studentService.summaryOf(STUDENT)).thenReturn(AssignmentFixtures.student(STUDENT));
        when(gradeService.visibleTo(any(), eq(GradeSource.ASSIGNMENT), any(), any())).thenReturn(Map.of());
    }

    private void givenTheSubmissionIsStored() {
        when(submissionRepository.save(any(Submission.class))).thenAnswer(call -> {
            Submission submission = call.getArgument(0);
            ReflectionTestUtils.setField(submission, "id", UUID.randomUUID());
            return submission;
        });
    }
}
