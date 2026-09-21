package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.assignment.dto.GradeResponse;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
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

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID TEACHER_PERSON = UUID.randomUUID();

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private UserService userService;

    private GradeService gradeService;

    @BeforeEach
    void setUp() {
        gradeService = new GradeService(gradeRepository, courseService, userService,
                Clock.fixed(AssignmentFixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    void the_teacher_reads_the_whole_record_of_its_course() {
        when(courseService.memberOf("juan", COURSE))
                .thenReturn(new CourseMember(COURSE, true, null));
        when(gradeRepository.findByCourseIdOrderByGradedAtDesc(COURSE))
                .thenReturn(List.of(grade(new BigDecimal("18.00"))));

        List<GradeResponse> grades = gradeService.ofCourse("juan", COURSE);

        assertThat(grades).singleElement()
                .satisfies(found -> assertThat(found.score())
                        .isEqualTo(new BigDecimal("18.00")));
        verify(gradeRepository, never())
                .findByCourseIdAndStudentIdOrderByGradedAtDesc(COURSE, STUDENT);
    }

    @Test
    void the_student_only_reads_its_own_grades() {
        when(courseService.memberOf("ana.estudiante", COURSE))
                .thenReturn(new CourseMember(COURSE, false, STUDENT));
        when(gradeRepository.findByCourseIdAndStudentIdOrderByGradedAtDesc(COURSE, STUDENT))
                .thenReturn(List.of(grade(new BigDecimal("14.50"))));

        assertThat(gradeService.ofCourse("ana.estudiante", COURSE)).singleElement()
                .satisfies(found -> assertThat(found.studentId()).isEqualTo(STUDENT));
        verify(gradeRepository, never()).findByCourseIdOrderByGradedAtDesc(COURSE);
    }

    @Test
    void grading_the_same_source_twice_rewrites_the_grade_instead_of_adding_another() {
        UUID submissionId = UUID.randomUUID();
        Grade existing = grade(new BigDecimal("12.00"));
        when(gradeRepository.findBySourceTypeAndSourceId(GradeSource.ASSIGNMENT, submissionId))
                .thenReturn(Optional.of(existing));
        when(userService.actor("juan"))
                .thenReturn(new Actor(TEACHER_PERSON, "juan", Map.of(Role.TEACHER, UUID.randomUUID())));

        Grade recorded = gradeService.record("juan", GradeSource.ASSIGNMENT, submissionId,
                STUDENT, COURSE, new BigDecimal("17.00"), "Mejoro mucho");

        assertThat(recorded).isSameAs(existing);
        assertThat(recorded.getScore()).isEqualTo(new BigDecimal("17.00"));
        assertThat(recorded.getFeedback()).isEqualTo("Mejoro mucho");
        assertThat(recorded.getGradedBy()).isEqualTo(TEACHER_PERSON);
        assertThat(recorded.getGradedAt()).isEqualTo(AssignmentFixtures.NOW);
        verify(gradeRepository, never()).save(existing);
    }

    private Grade grade(BigDecimal score) {
        Grade grade = Grade.of(GradeSource.ASSIGNMENT, UUID.randomUUID(), STUDENT, COURSE);
        grade.record(score, null, TEACHER_PERSON, AssignmentFixtures.NOW);
        return grade;
    }
}
