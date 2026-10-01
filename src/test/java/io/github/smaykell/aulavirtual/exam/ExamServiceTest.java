package io.github.smaykell.aulavirtual.exam;

import static io.github.smaykell.aulavirtual.exam.ExamFixtures.COURSE;
import static io.github.smaykell.aulavirtual.exam.ExamFixtures.UNIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.exam.dto.ExamData;
import io.github.smaykell.aulavirtual.exam.dto.ExamQuestionData;
import io.github.smaykell.aulavirtual.exam.dto.ExamQuestionResponse;
import io.github.smaykell.aulavirtual.exam.dto.ExamQuestionsData;
import io.github.smaykell.aulavirtual.exam.dto.ExamResponse;
import io.github.smaykell.aulavirtual.exam.exception.QuestionNotFoundException;
import io.github.smaykell.aulavirtual.exam.question.QuestionService;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.GradingSchemeService;
import io.github.smaykell.aulavirtual.gradebook.exception.CategoryNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private ExamAttemptRepository attemptRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private CourseService courseService;

    @Mock
    private GradeService gradeService;

    @Mock
    private GradingSchemeService gradingSchemeService;

    private ExamService examService;

    @BeforeEach
    void setUp() {
        examService = new ExamService(examRepository, examQuestionRepository, attemptRepository,
                questionService,
                courseService, gradeService, gradingSchemeService);
    }

    @Test
    void the_teacher_schedules_an_exam_in_a_unit_of_its_course() {
        when(courseService.courseOf(UNIT)).thenReturn(COURSE);
        when(examRepository.save(any(Exam.class))).thenAnswer(call -> call.getArgument(0));

        ExamResponse created = examService.create("ana", UNIT, ExamFixtures.data());

        verify(courseService).requireWritable("ana", COURSE);
        assertThat(created.courseId()).isEqualTo(COURSE);
        assertThat(created.opensAt()).isEqualTo(ExamFixtures.OPENS);
        assertThat(created.closesAt()).isEqualTo(ExamFixtures.CLOSES);
        assertThat(created.maxScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.questionCount()).isZero();
    }

    @Test
    void an_exam_that_closes_before_it_opens_is_rejected() {
        when(courseService.courseOf(UNIT)).thenReturn(COURSE);

        ApiException error = assertThrows(ApiException.class, () -> examService.create("ana",
                UNIT, ExamFixtures.window(ExamFixtures.CLOSES, ExamFixtures.OPENS)));

        assertThat(error.getCode()).isEqualTo("EXM_INVALID_WINDOW");
        verify(examRepository, never()).save(any());
    }

    @Test
    void the_category_of_an_exam_belongs_to_its_course() {
        UUID category = UUID.randomUUID();
        when(courseService.courseOf(UNIT)).thenReturn(COURSE);
        doThrow(new CategoryNotFoundException(category)).when(gradingSchemeService)
                .requireCategoryIn(COURSE, category);
        ExamData data = ExamFixtures.data();
        ExamData categorized = new ExamData(data.title(), data.instructions(), data.opensAt(),
                data.closesAt(), data.timeLimitMinutes(), data.maxAttempts(),
                data.shuffleQuestions(), data.shuffleOptions(), data.showsAnswers(), category);

        assertThrows(CategoryNotFoundException.class,
                () -> examService.create("ana", UNIT, categorized));

        verify(examRepository, never()).save(any());
    }

    @Test
    void the_questions_of_an_exam_are_replaced_in_order_and_add_up_to_its_score() {
        Exam exam = givenTheExam();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(examQuestionRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        when(questionService.byId(List.of(first, second))).thenReturn(Map.of());

        List<ExamQuestionResponse> placed = examService.replaceQuestions("ana", exam.getId(),
                new ExamQuestionsData(List.of(
                        new ExamQuestionData(first, new BigDecimal("12.00")),
                        new ExamQuestionData(second, new BigDecimal("8.00")))));

        InOrder order = inOrder(questionService, examQuestionRepository);
        order.verify(questionService).requireInBank(COURSE, List.of(first, second));
        order.verify(examQuestionRepository).deleteByExamId(exam.getId());
        order.verify(examQuestionRepository).saveAll(anyList());
        assertThat(placed).extracting(ExamQuestionResponse::position).containsExactly(1, 2);
        assertThat(exam.getMaxScore()).isEqualByComparingTo("20.00");
    }

    @Test
    void a_question_twice_in_the_same_exam_is_rejected() {
        Exam exam = givenTheExam();
        UUID question = UUID.randomUUID();

        ApiException error = assertThrows(ApiException.class,
                () -> examService.replaceQuestions("ana", exam.getId(),
                        new ExamQuestionsData(List.of(
                                new ExamQuestionData(question, BigDecimal.ONE),
                                new ExamQuestionData(question, BigDecimal.TEN)))));

        assertThat(error.getCode()).isEqualTo("EXM_REPEATED_QUESTION");
        verify(examQuestionRepository, never()).deleteByExamId(any());
    }

    @Test
    void a_question_outside_the_bank_leaves_the_exam_untouched() {
        Exam exam = givenTheExam();
        UUID foreign = UUID.randomUUID();
        doThrow(new QuestionNotFoundException(foreign)).when(questionService)
                .requireInBank(COURSE, List.of(foreign));

        assertThrows(QuestionNotFoundException.class,
                () -> examService.replaceQuestions("ana", exam.getId(), new ExamQuestionsData(
                        List.of(new ExamQuestionData(foreign, BigDecimal.ONE)))));

        verify(examQuestionRepository, never()).deleteByExamId(any());
        assertThat(exam.getMaxScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void the_questions_of_an_exam_already_taken_stay_as_they_are() {
        Exam exam = givenTheExam();
        when(attemptRepository.existsByExamId(exam.getId())).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> examService.replaceQuestions("ana", exam.getId(), new ExamQuestionsData(
                        List.of(new ExamQuestionData(UUID.randomUUID(), BigDecimal.ONE)))));

        assertThat(error.getCode()).isEqualTo("EXM_HAS_ATTEMPTS");
        verify(examQuestionRepository, never()).deleteByExamId(any());
    }

    @Test
    void an_exam_with_attempts_is_not_deleted() {
        Exam exam = givenTheExam();
        when(attemptRepository.existsByExamId(exam.getId())).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> examService.delete("ana", exam.getId()));

        assertThat(error.getCode()).isEqualTo("EXM_HAS_WORK");
        verify(examRepository, never()).delete(any());
    }

    @Test
    void a_student_does_not_see_the_questions_with_their_answers() {
        Exam exam = givenTheExam();
        when(courseService.memberOf("luis", COURSE))
                .thenReturn(new CourseMember(COURSE, false, UUID.randomUUID()));

        ApiException error = assertThrows(ApiException.class,
                () -> examService.questions("luis", exam.getId()));

        assertThat(error.getCode()).isEqualTo("EXM_ANSWERS_REQUIRE_STAFF");
        verify(examQuestionRepository, never()).findByExamIdOrderByPosition(any());
    }

    @Test
    void an_exam_with_grades_is_not_deleted() {
        Exam exam = givenTheExam();
        when(gradeService.anyFor(GradeSource.EXAM, exam.getId())).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> examService.delete("ana", exam.getId()));

        assertThat(error.getCode()).isEqualTo("EXM_HAS_WORK");
        verify(examRepository, never()).delete(any());
    }

    @Test
    void an_unknown_exam_answers_not_found() {
        UUID unknown = UUID.randomUUID();
        when(examRepository.findById(unknown)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> examService.get("ana", unknown));

        assertThat(error.getCode()).isEqualTo("EXM_NOT_FOUND");
    }

    private Exam givenTheExam() {
        Exam exam = ExamFixtures.exam();
        ReflectionTestUtils.setField(exam, "maxScore", BigDecimal.ZERO);
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        return exam;
    }
}
