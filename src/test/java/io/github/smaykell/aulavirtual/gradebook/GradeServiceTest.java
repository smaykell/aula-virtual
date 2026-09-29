package io.github.smaykell.aulavirtual.gradebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeEntry;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.UserService;
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

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-10T09:00:00Z");
    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID ASSIGNMENT = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID TEACHER_PERSON = UUID.randomUUID();
    private static final BigDecimal MAX_SCORE = new BigDecimal("20.00");

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private UserService userService;

    private GradeService gradeService;

    @BeforeEach
    void setUp() {
        gradeService = new GradeService(gradeRepository, userService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void grading_the_same_student_twice_rewrites_the_grade_instead_of_adding_another() {
        Grade existing = grade(new BigDecimal("12.00"));
        when(gradeRepository.findBySourceTypeAndSourceIdAndStudentId(GradeSource.ASSIGNMENT,
                ASSIGNMENT, STUDENT)).thenReturn(Optional.of(existing));
        givenTheTeacherGrades();

        GradeResponse recorded = gradeService.record("juan",
                entry(new BigDecimal("17.00"), "Mejoro mucho"));

        assertThat(recorded.score()).isEqualTo(new BigDecimal("17.00"));
        assertThat(existing.getFeedback()).isEqualTo("Mejoro mucho");
        assertThat(existing.getGradedBy()).isEqualTo(TEACHER_PERSON);
        assertThat(existing.getGradedAt()).isEqualTo(NOW);
        verify(gradeRepository, never()).save(existing);
    }

    @Test
    void the_first_grade_of_a_student_keeps_the_maximum_it_was_graded_against() {
        when(gradeRepository.findBySourceTypeAndSourceIdAndStudentId(GradeSource.ASSIGNMENT,
                ASSIGNMENT, STUDENT)).thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(call -> call.getArgument(0));
        givenTheTeacherGrades();

        GradeResponse recorded = gradeService.record("juan",
                entry(new BigDecimal("15.00"), null));

        assertThat(recorded.maxScore()).isEqualTo(MAX_SCORE);
        assertThat(recorded.sourceId()).isEqualTo(ASSIGNMENT);
        assertThat(recorded.studentId()).isEqualTo(STUDENT);
    }

    @Test
    void a_score_over_the_maximum_is_rejected_before_touching_anything() {
        ApiException error = assertThrows(ApiException.class,
                () -> gradeService.record("juan", entry(new BigDecimal("21.00"), null)));

        assertThat(error.getCode()).isEqualTo("GRB_SCORE_OUT_OF_RANGE");
        assertThat(error.getMessage()).contains("20.00");
        verifyNoInteractions(gradeRepository, userService);
    }

    @Test
    void nobody_grades_its_own_work() {
        when(userService.actor("ana")).thenReturn(new Actor(TEACHER_PERSON, "ana",
                Map.of(Role.ADMIN, UUID.randomUUID(), Role.STUDENT, STUDENT)));

        ApiException error = assertThrows(ApiException.class,
                () -> gradeService.record("ana", entry(MAX_SCORE, null)));

        assertThat(error.getCode()).isEqualTo("GRB_OWN_GRADE");
        verifyNoInteractions(gradeRepository);
    }

    @Test
    void the_staff_sees_a_draft_grade_keyed_by_student() {
        when(gradeRepository.findBySourceTypeAndSourceIdAndStudentIdIn(GradeSource.ASSIGNMENT,
                ASSIGNMENT, List.of(STUDENT))).thenReturn(List.of(grade(new BigDecimal("11.00"))));

        Map<UUID, GradeResponse> grades = gradeService.visibleTo(
                new CourseMember(COURSE, true, null), GradeSource.ASSIGNMENT, ASSIGNMENT,
                List.of(STUDENT));

        assertThat(grades.get(STUDENT).score()).isEqualTo(new BigDecimal("11.00"));
        assertThat(grades.get(STUDENT).returnedAt()).isNull();
    }

    @Test
    void the_student_does_not_see_a_draft_grade() {
        when(gradeRepository.findBySourceTypeAndSourceIdAndStudentIdIn(GradeSource.ASSIGNMENT,
                ASSIGNMENT, List.of(STUDENT))).thenReturn(List.of(grade(new BigDecimal("11.00"))));

        assertThat(gradeService.visibleTo(new CourseMember(COURSE, false, STUDENT),
                GradeSource.ASSIGNMENT, ASSIGNMENT, List.of(STUDENT))).isEmpty();
    }

    @Test
    void handing_back_keeps_the_first_moment_a_grade_was_returned() {
        Grade draft = grade(new BigDecimal("16.00"));
        Grade earlier = returned(new BigDecimal("12.00"));
        Instant firstReturn = earlier.getReturnedAt();
        when(gradeRepository.findBySourceTypeAndSourceIdAndStudentIdIn(GradeSource.ASSIGNMENT,
                ASSIGNMENT, List.of(STUDENT))).thenReturn(List.of(draft, earlier));

        gradeService.handBack(GradeSource.ASSIGNMENT, ASSIGNMENT, List.of(STUDENT));

        assertThat(draft.getReturnedAt()).isEqualTo(NOW);
        assertThat(earlier.getReturnedAt()).isEqualTo(firstReturn);
    }

    private void givenTheTeacherGrades() {
        when(userService.actor("juan")).thenReturn(
                new Actor(TEACHER_PERSON, "juan", Map.of(Role.TEACHER, UUID.randomUUID())));
    }

    private static GradeEntry entry(BigDecimal score, String feedback) {
        return new GradeEntry(GradeSource.ASSIGNMENT, ASSIGNMENT, COURSE, STUDENT, MAX_SCORE,
                score, feedback);
    }

    private static Grade returned(BigDecimal score) {
        Grade grade = grade(score);
        grade.handBack(NOW.minusSeconds(86400));
        return grade;
    }

    private static Grade grade(BigDecimal score) {
        Grade grade = Grade.of(GradeSource.ASSIGNMENT, ASSIGNMENT, STUDENT, COURSE);
        grade.record(score, MAX_SCORE, null, TEACHER_PERSON, NOW);
        return grade;
    }
}
