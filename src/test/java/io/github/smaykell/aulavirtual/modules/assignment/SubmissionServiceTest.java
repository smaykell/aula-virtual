package io.github.smaykell.aulavirtual.modules.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.assignment.dto.GradeData;
import io.github.smaykell.aulavirtual.modules.assignment.dto.SubmissionData;
import io.github.smaykell.aulavirtual.modules.assignment.dto.SubmissionResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.modules.student.StudentService;
import java.math.BigDecimal;
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
    private static final UUID UNIT = UUID.randomUUID();
    private static final UUID COURSE = UUID.randomUUID();
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

    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(submissionRepository, assignmentService,
                gradeService, studentService, Clock.fixed(NOW, ZoneOffset.UTC));
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
        Assignment assignment = AssignmentFixtures.assignment(UNIT);
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
        Assignment assignment = AssignmentFixtures.assignment(UNIT);
        Submission submission = AssignmentFixtures.submission(assignment.getId(), STUDENT, NOW,
                SubmissionStatus.SUBMITTED);
        when(assignmentService.existing(assignment.getId())).thenReturn(assignment);
        when(assignmentService.memberFor("ana.estudiante", assignment))
                .thenReturn(new CourseMember(COURSE, false, STUDENT));
        when(submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), STUDENT,
                FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(submission), FIRST_PAGE, 1));
        when(studentService.summariesOf(List.of(STUDENT)))
                .thenReturn(Map.of(STUDENT, AssignmentFixtures.student(STUDENT)));
        when(gradeService.bySource(eq(GradeSource.ASSIGNMENT), any())).thenReturn(Map.of());

        PageResponse<SubmissionResponse> page = submissionService.list("ana.estudiante",
                assignment.getId(), null, FIRST_PAGE);

        assertThat(page.content()).singleElement()
                .satisfies(found -> assertThat(found.student().id()).isEqualTo(STUDENT));
        verify(submissionRepository, never())
                .findByAssignmentId(assignment.getId(), FIRST_PAGE);
    }

    @Test
    void grading_leaves_the_submission_graded_and_writes_the_score() {
        Assignment assignment = AssignmentFixtures.assignment(UNIT);
        Submission submission = AssignmentFixtures.submission(assignment.getId(), STUDENT, NOW,
                SubmissionStatus.SUBMITTED);
        givenTheSubmissionIsManaged(assignment, submission);
        when(assignmentService.courseOf(assignment)).thenReturn(COURSE);
        when(gradeService.record(eq("juan"), eq(GradeSource.ASSIGNMENT), eq(submission.getId()),
                eq(STUDENT), eq(COURSE), eq(new BigDecimal("18.00")), eq("Buen trabajo")))
                .thenReturn(gradeOf(submission, new BigDecimal("18.00")));
        when(studentService.summaryOf(STUDENT))
                .thenReturn(AssignmentFixtures.student(STUDENT));

        SubmissionResponse graded = submissionService.grade("juan", submission.getId(),
                new GradeData(new BigDecimal("18.00"), "Buen trabajo"));

        assertThat(graded.status()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(graded.grade().score()).isEqualTo(new BigDecimal("18.00"));
        assertThat(submission.isGraded()).isTrue();
    }

    @Test
    void a_score_over_the_maximum_of_the_task_is_rejected() {
        Assignment assignment = AssignmentFixtures.assignment(UNIT);
        Submission submission = AssignmentFixtures.submission(assignment.getId(), STUDENT, NOW,
                SubmissionStatus.SUBMITTED);
        givenTheSubmissionIsManaged(assignment, submission);

        ApiException error = assertThrows(ApiException.class,
                () -> submissionService.grade("juan", submission.getId(),
                        new GradeData(new BigDecimal("21.00"), null)));

        assertThat(error.getCode()).isEqualTo("ASG_SCORE_OUT_OF_RANGE");
        assertThat(error.getMessage()).contains("20.00");
        assertThat(submission.isGraded()).isFalse();
    }

    @Test
    void an_unknown_submission_is_not_found() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> submissionService.grade("juan", submissionId,
                        new GradeData(BigDecimal.ONE, null)));

        assertThat(error.getCode()).isEqualTo("ASG_SUBMISSION_NOT_FOUND");
    }

    private Assignment givenTheAssignmentFor(String actorUsername, UUID studentId) {
        return givenTheAssignmentFor(actorUsername, studentId, AssignmentFixtures.DUE_AT, false);
    }

    private Assignment givenTheAssignmentFor(String actorUsername, UUID studentId, Instant dueAt,
            boolean allowsLate) {

        Assignment assignment = AssignmentFixtures.assignment(UNIT, dueAt, allowsLate);
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

    private void givenTheSubmissionIsManaged(Assignment assignment, Submission submission) {
        when(submissionRepository.findById(submission.getId()))
                .thenReturn(Optional.of(submission));
        when(assignmentService.writable("juan", assignment.getId())).thenReturn(assignment);
    }

    private void givenTheStudentAndItsGrades() {
        when(studentService.summaryOf(STUDENT)).thenReturn(AssignmentFixtures.student(STUDENT));
        when(gradeService.bySource(eq(GradeSource.ASSIGNMENT), any())).thenReturn(Map.of());
    }

    private void givenTheSubmissionIsStored() {
        when(submissionRepository.save(any(Submission.class))).thenAnswer(call -> {
            Submission submission = call.getArgument(0);
            ReflectionTestUtils.setField(submission, "id", UUID.randomUUID());
            return submission;
        });
    }

    private Grade gradeOf(Submission submission, BigDecimal score) {
        Grade grade = Grade.of(GradeSource.ASSIGNMENT, submission.getId(), STUDENT, COURSE);
        ReflectionTestUtils.setField(grade, "id", UUID.randomUUID());
        grade.record(score, "Buen trabajo", UUID.randomUUID(), NOW);
        return grade;
    }
}
