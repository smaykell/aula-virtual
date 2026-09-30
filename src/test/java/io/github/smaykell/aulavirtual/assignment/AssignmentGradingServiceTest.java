package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.assignment.dto.GradeData;
import io.github.smaykell.aulavirtual.assignment.dto.HandBackRequest;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.exception.StudentNotEnrolledException;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeEntry;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentGradingServiceTest {

    private static final UUID COURSE = AssignmentFixtures.COURSE;
    private static final UUID STUDENT = UUID.randomUUID();
    private static final GradeData EIGHTEEN = new GradeData(new BigDecimal("18.00"),
            "Buen trabajo");

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private GradeService gradeService;

    @Mock
    private AssignmentNotices notices;

    private AssignmentGradingService gradingService;

    @BeforeEach
    void setUp() {
        gradingService = new AssignmentGradingService(assignmentService, submissionRepository,
                courseService, gradeService, notices);
    }

    @Test
    void a_student_who_handed_nothing_in_is_graded_all_the_same() {
        Assignment assignment = givenTheWritableAssignment();
        when(gradeService.record(eq("juan"), any(GradeEntry.class))).thenReturn(aGrade());

        GradeResponse grade = gradingService.grade("juan", assignment.getId(), STUDENT,
                EIGHTEEN);

        assertThat(grade.score()).isEqualTo(new BigDecimal("18.00"));
    }

    @Test
    void the_grade_is_recorded_against_the_task_its_course_and_its_maximum() {
        Assignment assignment = givenTheWritableAssignment();
        ArgumentCaptor<GradeEntry> entry = ArgumentCaptor.forClass(GradeEntry.class);
        when(gradeService.record(eq("juan"), entry.capture())).thenReturn(aGrade());

        gradingService.grade("juan", assignment.getId(), STUDENT, EIGHTEEN);

        assertThat(entry.getValue()).isEqualTo(new GradeEntry(GradeSource.ASSIGNMENT,
                assignment.getId(), COURSE, STUDENT, AssignmentFixtures.MAX_SCORE,
                new BigDecimal("18.00"), "Buen trabajo"));
    }

    @Test
    void someone_outside_the_course_is_not_graded() {
        Assignment assignment = givenTheWritableAssignment();
        doThrow(new StudentNotEnrolledException()).when(courseService)
                .requireActiveStudent(COURSE, STUDENT);

        ApiException error = assertThrows(ApiException.class,
                () -> gradingService.grade("juan", assignment.getId(), STUDENT, EIGHTEEN));

        assertThat(error.getCode()).isEqualTo("CRS_STUDENT_NOT_ENROLLED");
        verify(gradeService, never()).record(any(), any());
    }

    @Test
    void grading_leaves_the_submission_open_until_the_grade_is_handed_back() {
        Assignment assignment = givenTheWritableAssignment();
        when(gradeService.record(eq("juan"), any(GradeEntry.class))).thenReturn(aGrade());

        gradingService.grade("juan", assignment.getId(), STUDENT, EIGHTEEN);

        verifyNoInteractions(submissionRepository);
    }

    @Test
    void handing_back_closes_the_submissions_of_the_students_who_got_their_grade() {
        Assignment assignment = givenTheWritableAssignment();
        UUID withoutGrade = UUID.randomUUID();
        Submission submission = AssignmentFixtures.submission(assignment.getId(), STUDENT,
                AssignmentFixtures.NOW, SubmissionStatus.SUBMITTED);
        when(gradeService.handBack(GradeSource.ASSIGNMENT, assignment.getId(),
                List.of(STUDENT, withoutGrade))).thenReturn(List.of(aGrade()));
        when(submissionRepository.findByAssignmentIdAndStudentIdIn(assignment.getId(),
                List.of(STUDENT))).thenReturn(List.of(submission));

        List<GradeResponse> returned = gradingService.handBack("juan", assignment.getId(),
                new HandBackRequest(List.of(STUDENT, withoutGrade)));

        assertThat(returned).extracting(GradeResponse::studentId).containsExactly(STUDENT);
        assertThat(submission.isGraded()).isTrue();
        verify(notices).handedBack(assignment, List.of(STUDENT));
    }

    private Assignment givenTheWritableAssignment() {
        Assignment assignment = AssignmentFixtures.assignment();
        when(assignmentService.writable("juan", assignment.getId())).thenReturn(assignment);
        return assignment;
    }

    private static GradeResponse aGrade() {
        return new GradeResponse(UUID.randomUUID(), GradeSource.ASSIGNMENT, UUID.randomUUID(),
                STUDENT, COURSE, new BigDecimal("18.00"), AssignmentFixtures.MAX_SCORE,
                "Buen trabajo", Instant.EPOCH, Instant.EPOCH);
    }
}
