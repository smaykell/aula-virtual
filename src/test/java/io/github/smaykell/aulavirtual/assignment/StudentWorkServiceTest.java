package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentService;
import io.github.smaykell.aulavirtual.assignment.dto.StudentWorkResponse;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentWorkServiceTest {

    private static final UUID COURSE = AssignmentFixtures.COURSE;
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUIS = UUID.randomUUID();

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private StudentService studentService;

    @Mock
    private GradeService gradeService;

    @Mock
    private AttachmentService attachmentService;

    private StudentWorkService studentWorkService;

    @BeforeEach
    void setUp() {
        studentWorkService = new StudentWorkService(assignmentService, submissionRepository,
                courseService, studentService, gradeService, attachmentService);
    }

    @Test
    void the_teacher_sees_every_active_student_whether_they_handed_in_or_not() {
        Assignment assignment = givenTheAssignmentReadBy("juan",
                new CourseMember(COURSE, true, null));
        List<UUID> students = List.of(ANA, LUIS);
        when(courseService.activeStudentsOf(COURSE)).thenReturn(students);
        when(studentService.summariesOf(students)).thenReturn(Map.of(
                ANA, student(ANA, "Ana", "Quispe Rojas"),
                LUIS, student(LUIS, "Luis", "Álvarez Ruiz")));
        when(submissionRepository.findByAssignmentIdAndStudentIdIn(assignment.getId(), students))
                .thenReturn(List.of(AssignmentFixtures.submission(assignment.getId(), ANA,
                        AssignmentFixtures.NOW, SubmissionStatus.SUBMITTED)));
        CourseMember staff = new CourseMember(COURSE, true, null);
        when(gradeService.visibleTo(staff, GradeSource.ASSIGNMENT, assignment.getId(), students))
                .thenReturn(Map.of(LUIS, grade(LUIS)));

        List<StudentWorkResponse> work = studentWorkService.of("juan", assignment.getId());

        assertThat(work).extracting(row -> row.student().lastName())
                .containsExactly("Álvarez Ruiz", "Quispe Rojas");
        assertThat(work.get(0).submission()).isNull();
        assertThat(work.get(0).grade().score()).isEqualByComparingTo("0");
        assertThat(work.get(1).submission().text()).isEqualTo("Mi respuesta");
        assertThat(work.get(1).grade()).isNull();
    }

    @Test
    void the_student_only_sees_its_own_work() {
        CourseMember ana = new CourseMember(COURSE, false, ANA);
        Assignment assignment = givenTheAssignmentReadBy("ana.estudiante", ana);
        when(studentService.summariesOf(List.of(ANA)))
                .thenReturn(Map.of(ANA, student(ANA, "Ana", "Quispe Rojas")));
        when(submissionRepository.findByAssignmentIdAndStudentIdIn(assignment.getId(),
                List.of(ANA))).thenReturn(List.of());
        when(gradeService.visibleTo(ana, GradeSource.ASSIGNMENT, assignment.getId(),
                List.of(ANA))).thenReturn(Map.of());

        assertThat(studentWorkService.of("ana.estudiante", assignment.getId())).singleElement()
                .satisfies(row -> assertThat(row.student().id()).isEqualTo(ANA));
        verify(courseService, never()).activeStudentsOf(COURSE);
    }

    private Assignment givenTheAssignmentReadBy(String actorUsername, CourseMember member) {
        Assignment assignment = AssignmentFixtures.assignment();
        when(assignmentService.existing(assignment.getId())).thenReturn(assignment);
        when(assignmentService.memberFor(actorUsername, assignment)).thenReturn(member);
        return assignment;
    }

    private static StudentSummary student(UUID id, String firstName, String lastName) {
        return new StudentSummary(id, firstName, lastName, "Hospital Regional", true);
    }

    private static GradeResponse grade(UUID studentId) {
        return new GradeResponse(UUID.randomUUID(), GradeSource.ASSIGNMENT, UUID.randomUUID(),
                studentId, COURSE, BigDecimal.ZERO, AssignmentFixtures.MAX_SCORE, null,
                Instant.EPOCH, null);
    }
}
