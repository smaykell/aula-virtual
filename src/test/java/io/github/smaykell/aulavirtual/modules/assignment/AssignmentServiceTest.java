package io.github.smaykell.aulavirtual.modules.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.assignment.dto.AssignmentResponse;
import io.github.smaykell.aulavirtual.modules.course.CourseService;
import io.github.smaykell.aulavirtual.modules.course.UnitService;
import io.github.smaykell.aulavirtual.modules.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.modules.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.modules.course.exception.CourseOutOfReachException;
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

    private static final UUID UNIT = UUID.randomUUID();
    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private UnitService unitService;

    @Mock
    private CourseService courseService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(assignmentRepository, unitService,
                courseService);
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
        Assignment assignment = AssignmentFixtures.assignment(UNIT);
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
    void an_unknown_task_is_not_found() {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> assignmentService.get("juan", assignmentId));

        assertThat(error.getCode()).isEqualTo("ASG_NOT_FOUND");
    }

    private Assignment givenTheWritableAssignment() {
        Assignment assignment = AssignmentFixtures.assignment(UNIT);
        when(assignmentRepository.findById(assignment.getId()))
                .thenReturn(Optional.of(assignment));
        when(unitService.courseOf(UNIT)).thenReturn(COURSE);
        return assignment;
    }
}
