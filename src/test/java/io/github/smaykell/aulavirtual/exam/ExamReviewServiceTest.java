package io.github.smaykell.aulavirtual.exam;

import static io.github.smaykell.aulavirtual.exam.ExamFixtures.COURSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.course.exception.CourseOutOfReachException;
import io.github.smaykell.aulavirtual.exam.dto.ExamResultResponse;
import io.github.smaykell.aulavirtual.exam.dto.HandBackData;
import io.github.smaykell.aulavirtual.exam.dto.ScoreData;
import io.github.smaykell.aulavirtual.exam.question.QuestionType;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExamReviewServiceTest {

    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID OPEN = UUID.randomUUID();

    @Mock
    private ExamService examService;

    @Mock
    private AttemptService attemptService;

    @Mock
    private ExamAttemptRepository attemptRepository;

    @Mock
    private AttemptAnswerRepository answerRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private StudentService studentService;

    @Mock
    private GradeService gradeService;

    @Mock
    private ExamGrading grading;

    private ExamReviewService reviewService;

    private Exam exam;

    private ExamAttempt attempt;

    private AttemptAnswer written;

    @BeforeEach
    void setUp() {
        reviewService = new ExamReviewService(examService, attemptService, attemptRepository,
                answerRepository, examQuestionRepository, courseService, studentService,
                gradeService, grading);
        exam = ExamFixtures.exam();
        exam.scoreOutOf(new BigDecimal("12.00"));
        attempt = ExamAttempt.start(exam, STUDENT, 1, ExamFixtures.OPENS, 7L);
        ReflectionTestUtils.setField(attempt, "id", UUID.randomUUID());
        written = AttemptAnswer.of(attempt.getId(), OPEN);
        written.replace(null, "Porque la Tierra rota");
        ExamQuestion placed = ExamQuestion.of(exam.getId(), OPEN, 1, new BigDecimal("12.00"));

        lenient().when(attemptService.locked(attempt.getId())).thenReturn(attempt);
        lenient().when(examService.writable("ana", exam.getId())).thenReturn(exam);
        lenient().when(examQuestionRepository.findByExamIdAndQuestionId(exam.getId(), OPEN))
                .thenReturn(Optional.of(placed));
        lenient().when(answerRepository.findByAttemptIdAndQuestionId(attempt.getId(), OPEN))
                .thenReturn(Optional.of(written));
        lenient().when(attemptService.paperOf(exam, attempt)).thenAnswer(call ->
                new AttemptPaper(exam, attempt, List.of(placed), Map.of(OPEN,
                        new QuestionResponse(OPEN, COURSE, QuestionType.SHORT_ANSWER,
                                "Explica el día y la noche", List.of(), null, Instant.EPOCH)),
                        Map.of(OPEN, written)));
    }

    @Test
    void scoring_the_last_open_answer_grades_the_attempt_in_the_name_of_the_teacher() {
        attempt.close(ExamFixtures.OPENS.plusSeconds(600), new Grading(BigDecimal.ZERO, true));

        reviewService.score("ana", attempt.getId(), OPEN,
                new ScoreData(new BigDecimal("10.00"), "Falta mencionar el eje"));

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.GRADED);
        assertThat(attempt.getScore()).isEqualByComparingTo("10.00");
        assertThat(written.getFeedback()).isEqualTo("Falta mencionar el eje");
        verify(grading).recordBestBy("ana", exam, STUDENT);
    }

    @Test
    void a_question_does_not_take_more_points_than_it_is_worth() {
        attempt.close(ExamFixtures.OPENS.plusSeconds(600), new Grading(BigDecimal.ZERO, true));

        ApiException error = assertThrows(ApiException.class, () -> reviewService.score("ana",
                attempt.getId(), OPEN, new ScoreData(new BigDecimal("12.50"), null)));

        assertThat(error.getCode()).isEqualTo("EXM_POINTS_OUT_OF_RANGE");
        assertThat(error.getMessage()).contains("12.00");
        assertThat(written.isScored()).isFalse();
    }

    @Test
    void an_attempt_in_progress_is_not_reviewed() {
        ApiException error = assertThrows(ApiException.class, () -> reviewService.score("ana",
                attempt.getId(), OPEN, new ScoreData(BigDecimal.ONE, null)));

        assertThat(error.getCode()).isEqualTo("EXM_ATTEMPT_IN_PROGRESS");
    }

    @Test
    void an_unanswered_question_has_nothing_to_score() {
        attempt.close(ExamFixtures.OPENS.plusSeconds(600), new Grading(BigDecimal.ZERO, false));
        when(answerRepository.findByAttemptIdAndQuestionId(attempt.getId(), OPEN))
                .thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class, () -> reviewService.score("ana",
                attempt.getId(), OPEN, new ScoreData(BigDecimal.ONE, null)));

        assertThat(error.getCode()).isEqualTo("EXM_NOT_ANSWERED");
    }

    @Test
    void only_who_can_write_the_course_scores() {
        doThrow(new CourseOutOfReachException()).when(examService)
                .writable("luis", exam.getId());

        assertThrows(CourseOutOfReachException.class, () -> reviewService.score("luis",
                attempt.getId(), OPEN, new ScoreData(BigDecimal.ONE, null)));

        assertThat(written.isScored()).isFalse();
    }

    @Test
    void the_staff_sees_every_enrolled_student_even_without_attempts() {
        UUID absent = UUID.randomUUID();
        when(examService.existing(exam.getId())).thenReturn(exam);
        CourseMember staff = new CourseMember(COURSE, true, null);
        when(examService.memberFor("ana", exam)).thenReturn(staff);
        when(courseService.activeStudentsOf(COURSE)).thenReturn(List.of(STUDENT, absent));
        when(attemptRepository.findByExamIdAndStudentIdInOrderByNumber(exam.getId(),
                List.of(STUDENT, absent))).thenReturn(List.of(attempt));
        when(studentService.summariesOf(anyList())).thenReturn(Map.of(
                STUDENT, new StudentSummary(STUDENT, "Luis", "Zapata", null, true),
                absent, new StudentSummary(absent, "Ana", "Alvarez", null, true)));
        when(gradeService.visibleTo(staff, GradeSource.EXAM, exam.getId(),
                List.of(STUDENT, absent))).thenReturn(Map.of());

        List<ExamResultResponse> results = reviewService.results("ana", exam.getId());

        assertThat(results).extracting(result -> result.student().lastName())
                .containsExactly("Alvarez", "Zapata");
        assertThat(results.getFirst().attempts()).isEmpty();
        assertThat(results.get(1).attempts()).hasSize(1);
    }

    @Test
    void handing_back_needs_to_write_the_course() {
        doThrow(new CourseOutOfReachException()).when(examService)
                .writable("luis", exam.getId());

        assertThrows(CourseOutOfReachException.class, () -> reviewService.handBack("luis",
                exam.getId(), new HandBackData(List.of(STUDENT))));

        verify(gradeService, never()).handBack(any(), any(), any());
    }
}
