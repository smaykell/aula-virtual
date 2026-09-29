package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.assignment.dto.AssignmentResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.course.exception.CourseOutOfReachException;
import io.github.smaykell.aulavirtual.course.unit.UnitService;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    private static final UUID UNIT = AssignmentFixtures.UNIT;
    private static final UUID COURSE = AssignmentFixtures.COURSE;
    private static final UUID STUDENT = UUID.randomUUID();

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private GradeService gradeService;

    @Mock
    private UnitService unitService;

    @Mock
    private CourseService courseService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(assignmentRepository, submissionRepository,
                unitService, courseService, gradeService);
    }

    @Test
    void the_teacher_publishes_a_task_in_a_unit_of_its_course() {
        when(unitService.courseOf(UNIT)).thenReturn(COURSE);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(call -> {
            Assignment assignment = call.getArgument(0);
            ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
            return assignment;
        });

        AssignmentResponse assignment = assignmentService.create("juan", UNIT,
                AssignmentFixtures.data());

        assertThat(assignment.title()).isEqualTo("Practica 1");
        assertThat(assignment.maxScore()).isEqualTo(AssignmentFixtures.MAX_SCORE);
        assertThat(assignment.allowsLate()).isFalse();
        verify(courseService).requireWritable("juan", COURSE);
        verify(assignmentRepository).save(argThat(saved -> COURSE.equals(saved.getCourseId())));
    }

    @Test
    void whoever_does_not_write_the_course_does_not_publish_tasks_in_it() {
        when(unitService.courseOf(UNIT)).thenReturn(COURSE);
        doThrow(new CourseOutOfReachException()).when(courseService)
                .requireWritable("otro", COURSE);

        ApiException error = assertThrows(ApiException.class,
                () -> assignmentService.create("otro", UNIT, AssignmentFixtures.data()));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void an_archived_course_does_not_take_new_tasks() {
        when(unitService.courseOf(UNIT)).thenReturn(COURSE);
        doThrow(new ArchivedCourseException()).when(courseService)
                .requireWritable("juan", COURSE);

        ApiException error = assertThrows(ApiException.class,
                () -> assignmentService.create("juan", UNIT, AssignmentFixtures.data()));

        assertThat(error.getCode()).isEqualTo("CRS_ARCHIVED");
    }

    @Test
    void an_enrolled_student_reads_the_tasks_of_the_unit() {
        Assignment assignment = AssignmentFixtures.assignment();
        when(unitService.courseOf(UNIT)).thenReturn(COURSE);
        when(courseService.memberOf("ana.estudiante", COURSE))
                .thenReturn(new CourseMember(COURSE, false, STUDENT));
        when(assignmentRepository.findByUnitIdOrderByDueAt(UNIT))
                .thenReturn(List.of(assignment));

        assertThat(assignmentService.list("ana.estudiante", UNIT)).singleElement()
                .satisfies(found -> assertThat(found.title()).isEqualTo("Practica 1"));
    }

    @Test
    void someone_outside_the_course_does_not_read_its_tasks() {
        when(unitService.courseOf(UNIT)).thenReturn(COURSE);
        when(courseService.memberOf("ajeno", COURSE))
                .thenThrow(new CourseOutOfReachException());

        ApiException error = assertThrows(ApiException.class,
                () -> assignmentService.list("ajeno", UNIT));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
    }

    @Test
    void updating_a_task_replaces_its_dates_and_its_score() {
        Assignment assignment = givenTheWritableAssignment();

        AssignmentResponse updated = assignmentService.update("juan", assignment.getId(),
                AssignmentFixtures.data(AssignmentFixtures.DUE_AT, true));

        assertThat(updated.allowsLate()).isTrue();
        assertThat(assignment.isAllowsLate()).isTrue();
    }

    @Test
    void deleting_a_task_takes_it_out_of_the_unit() {
        Assignment assignment = givenTheWritableAssignment();

        assignmentService.delete("juan", assignment.getId());

        verify(assignmentRepository).delete(assignment);
    }

    @Test
    void a_task_that_already_has_submissions_is_not_deleted() {
        Assignment assignment = givenTheWritableAssignment();
        when(submissionRepository.existsByAssignmentId(assignment.getId())).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> assignmentService.delete("juan", assignment.getId()));

        assertThat(error.getCode()).isEqualTo("ASG_HAS_WORK");
        verify(assignmentRepository, never()).delete(assignment);
    }

    @Test
    void a_task_graded_without_submissions_is_not_deleted_either() {
        Assignment assignment = givenTheWritableAssignment();
        when(gradeService.anyFor(GradeSource.ASSIGNMENT, assignment.getId())).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> assignmentService.delete("juan", assignment.getId()));

        assertThat(error.getCode()).isEqualTo("ASG_HAS_WORK");
        verify(assignmentRepository, never()).delete(assignment);
    }

    @Test
    void an_unknown_task_is_not_found() {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> assignmentService.get("juan", assignmentId));

        assertThat(error.getCode()).isEqualTo("ASG_NOT_FOUND");
    }

    private Assignment givenTheWritableAssignment() {
        Assignment assignment = AssignmentFixtures.assignment();
        when(assignmentRepository.findById(assignment.getId()))
                .thenReturn(Optional.of(assignment));
        return assignment;
    }
}
