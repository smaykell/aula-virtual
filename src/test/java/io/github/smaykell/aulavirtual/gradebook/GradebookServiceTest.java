package io.github.smaykell.aulavirtual.gradebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.course.exception.CourseOutOfReachException;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookRow;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
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
class GradebookServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-10T09:00:00Z");
    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUIS = UUID.randomUUID();
    private static final BigDecimal TWENTY = new BigDecimal("20.00");
    private static final GradeItem FIRST_TASK = item("Practica 1", NOW);
    private static final GradeItem SECOND_TASK = item("Practica 2", NOW.plusSeconds(86400));
    private static final GradingSchemeResponse TOTAL_POINTS = new GradingSchemeResponse(
            GradingMethod.TOTAL_POINTS, new BigDecimal("13.00"), List.of());

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private GradingSchemeService schemeService;

    @Mock
    private CourseService courseService;

    @Mock
    private StudentService studentService;

    private GradebookService gradebookService;

    @BeforeEach
    void setUp() {
        GradeItemProvider provider = courseId -> List.of(SECOND_TASK, FIRST_TASK);
        gradebookService = new GradebookService(gradeRepository, schemeService, courseService,
                studentService, List.of(provider));
    }

    @Test
    void the_teacher_sees_every_active_student_in_alphabetical_order_with_drafts_included() {
        givenTheTeacher();
        when(gradeRepository.findByCourseIdAndStudentIdIn(COURSE, List.of(LUIS, ANA)))
                .thenReturn(List.of(returned(ANA, FIRST_TASK, "16"), draft(ANA, SECOND_TASK, "10"),
                        draft(LUIS, FIRST_TASK, "12")));

        GradebookResponse gradebook = gradebookService.of("juan", COURSE);

        assertThat(gradebook.rows()).extracting(row -> row.student().lastName())
                .containsExactly("Álvarez Ruiz", "Quispe Rojas");
        GradebookRow ana = gradebook.rows().get(1);
        assertThat(ana.grades()).hasSize(2);
        assertThat(ana.finalGrade().score()).isEqualByComparingTo("13.00");
    }

    @Test
    void the_columns_come_ordered_by_their_due_date() {
        givenTheTeacher();
        when(gradeRepository.findByCourseIdAndStudentIdIn(COURSE, List.of(LUIS, ANA)))
                .thenReturn(List.of());

        assertThat(gradebookService.of("juan", COURSE).items())
                .containsExactly(FIRST_TASK, SECOND_TASK);
    }

    @Test
    void a_student_without_grades_has_a_row_but_no_final_grade() {
        givenTheTeacher();
        when(gradeRepository.findByCourseIdAndStudentIdIn(COURSE, List.of(LUIS, ANA)))
                .thenReturn(List.of());

        assertThat(gradebookService.of("juan", COURSE).rows())
                .allSatisfy(row -> assertThat(row.finalGrade()).isNull());
    }

    @Test
    void the_student_sees_only_its_row_and_only_what_was_handed_back() {
        when(courseService.memberOf("ana.estudiante", COURSE))
                .thenReturn(new CourseMember(COURSE, false, ANA));
        when(schemeService.schemeOf(COURSE)).thenReturn(TOTAL_POINTS);
        when(studentService.summariesOf(List.of(ANA)))
                .thenReturn(Map.of(ANA, student(ANA, "Ana", "Quispe Rojas")));
        when(gradeRepository.findByCourseIdAndStudentIdIn(COURSE, List.of(ANA)))
                .thenReturn(List.of(returned(ANA, FIRST_TASK, "16"),
                        draft(ANA, SECOND_TASK, "10")));

        GradebookResponse gradebook = gradebookService.of("ana.estudiante", COURSE);

        assertThat(gradebook.rows()).singleElement().satisfies(row -> {
            assertThat(row.grades()).singleElement()
                    .satisfies(grade -> assertThat(grade.sourceId())
                            .isEqualTo(FIRST_TASK.sourceId()));
            assertThat(row.finalGrade().score()).isEqualByComparingTo("16.00");
        });
        verify(courseService, never()).activeStudentsOf(COURSE);
    }

    @Test
    void a_grade_whose_task_no_longer_exists_does_not_count() {
        givenTheTeacher();
        GradeItem gone = item("Borrada", NOW);
        when(gradeRepository.findByCourseIdAndStudentIdIn(COURSE, List.of(LUIS, ANA)))
                .thenReturn(List.of(returned(ANA, FIRST_TASK, "20"), returned(ANA, gone, "0")));

        GradebookRow ana = gradebookService.of("juan", COURSE).rows().get(1);

        assertThat(ana.grades()).hasSize(1);
        assertThat(ana.finalGrade().score()).isEqualByComparingTo("20.00");
    }

    @Test
    void someone_outside_the_course_does_not_read_its_gradebook() {
        when(courseService.memberOf("ajeno", COURSE)).thenThrow(new CourseOutOfReachException());

        ApiException error = assertThrows(ApiException.class,
                () -> gradebookService.of("ajeno", COURSE));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
    }

    private void givenTheTeacher() {
        when(courseService.memberOf("juan", COURSE))
                .thenReturn(new CourseMember(COURSE, true, null));
        when(schemeService.schemeOf(COURSE)).thenReturn(TOTAL_POINTS);
        when(courseService.activeStudentsOf(COURSE)).thenReturn(List.of(LUIS, ANA));
        when(studentService.summariesOf(List.of(LUIS, ANA))).thenReturn(Map.of(
                ANA, student(ANA, "Ana", "Quispe Rojas"),
                LUIS, student(LUIS, "Luis", "Álvarez Ruiz")));
    }

    private static Grade draft(UUID studentId, GradeItem item, String score) {
        Grade grade = Grade.of(GradeSource.ASSIGNMENT, item.sourceId(), studentId, COURSE);
        grade.record(new BigDecimal(score), TWENTY, null, UUID.randomUUID(), NOW);
        return grade;
    }

    private static Grade returned(UUID studentId, GradeItem item, String score) {
        Grade grade = draft(studentId, item, score);
        grade.handBack(NOW);
        return grade;
    }

    private static GradeItem item(String title, Instant dueAt) {
        return new GradeItem(GradeSource.ASSIGNMENT, UUID.randomUUID(), title, null, TWENTY,
                dueAt);
    }

    private static StudentSummary student(UUID id, String firstName, String lastName) {
        return new StudentSummary(id, firstName, lastName, "Hospital Regional", true);
    }
}
