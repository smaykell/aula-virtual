package io.github.smaykell.aulavirtual.exam;

import static io.github.smaykell.aulavirtual.exam.ExamFixtures.COURSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.exam.dto.AnswerData;
import io.github.smaykell.aulavirtual.exam.dto.AttemptOptionResponse;
import io.github.smaykell.aulavirtual.exam.dto.AttemptQuestionResponse;
import io.github.smaykell.aulavirtual.exam.dto.AttemptResponse;
import io.github.smaykell.aulavirtual.exam.question.QuestionService;
import io.github.smaykell.aulavirtual.exam.question.QuestionType;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionResponse;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    private static final Instant NOW = ExamFixtures.OPENS.plusSeconds(600);
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID CHOICE = UUID.randomUUID();
    private static final UUID OPEN = UUID.randomUUID();
    private static final UUID RIGHT = UUID.randomUUID();
    private static final UUID WRONG = UUID.randomUUID();

    @Mock
    private ExamService examService;

    @Mock
    private ExamAttemptRepository attemptRepository;

    @Mock
    private AttemptAnswerRepository answerRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private ExamGrading grading;

    private final List<AttemptAnswer> stored = new ArrayList<>();

    private Exam exam;

    private AttemptService attemptService;

    @BeforeEach
    void setUp() {
        attemptService = new AttemptService(examService, attemptRepository, answerRepository,
                examQuestionRepository, questionService, grading,
                Clock.fixed(NOW, ZoneOffset.UTC));
        exam = ExamFixtures.exam();
        exam.scoreOutOf(new BigDecimal("20.00"));
        lenient().when(examService.existing(exam.getId())).thenReturn(exam);
        lenient().when(examService.memberFor("luis", exam))
                .thenReturn(new CourseMember(COURSE, false, STUDENT));
        lenient().when(examQuestionRepository.findByExamIdOrderByPosition(exam.getId()))
                .thenReturn(List.of(ExamQuestion.of(exam.getId(), CHOICE, 1, BigDecimal.TEN),
                        ExamQuestion.of(exam.getId(), OPEN, 2, BigDecimal.TEN)));
        lenient().when(questionService.byId(anyList())).thenReturn(Map.of(
                CHOICE, new QuestionResponse(CHOICE, COURSE, QuestionType.SINGLE_CHOICE,
                        "Capital del Perú", List.of(new OptionResponse(RIGHT, "Lima", true),
                        new OptionResponse(WRONG, "Cusco", false)), null, Instant.EPOCH),
                OPEN, new QuestionResponse(OPEN, COURSE, QuestionType.SHORT_ANSWER,
                        "Explica por qué", List.of(), "Porque sí", Instant.EPOCH)));
        lenient().when(answerRepository.findByAttemptId(any())).thenReturn(stored);
    }

    @Test
    void the_student_starts_its_first_attempt_with_the_deadline_set_by_the_server() {
        givenTheAttempts();
        when(attemptRepository.save(any(ExamAttempt.class)))
                .thenAnswer(call -> withId(call.getArgument(0)));

        AttemptResponse paper = attemptService.start("luis", exam.getId());

        assertThat(paper.number()).isEqualTo(1);
        assertThat(paper.deadline()).isEqualTo(NOW.plusSeconds(45 * 60));
        assertThat(paper.questions()).hasSize(2);
        assertThat(paper.questions()).flatExtracting(AttemptQuestionResponse::options)
                .extracting(AttemptOptionResponse::correct)
                .containsOnlyNulls();
        assertThat(paper.questions()).extracting(AttemptQuestionResponse::modelAnswer)
                .containsOnlyNulls();
    }

    @Test
    void starting_again_resumes_the_attempt_in_progress() {
        ExamAttempt inProgress = attempt(1, NOW.minusSeconds(60));
        givenTheAttempts(inProgress);

        AttemptResponse paper = attemptService.start("luis", exam.getId());

        assertThat(paper.id()).isEqualTo(inProgress.getId());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void nobody_starts_an_exam_that_is_not_open() {
        Instant early = ExamFixtures.OPENS.minusSeconds(60);
        attemptService = new AttemptService(examService, attemptRepository, answerRepository,
                examQuestionRepository, questionService, grading,
                Clock.fixed(early, ZoneOffset.UTC));
        givenTheAttempts();

        assertRejected(() -> attemptService.start("luis", exam.getId()), "EXM_NOT_OPEN");
    }

    @Test
    void a_single_attempt_exam_does_not_take_a_second_one() {
        ExamAttempt first = attempt(1, NOW.minusSeconds(1200));
        first.close(NOW.minusSeconds(600), new Grading(BigDecimal.TEN, false));
        givenTheAttempts(first);

        assertRejected(() -> attemptService.start("luis", exam.getId()),
                "EXM_NO_ATTEMPTS_LEFT");
    }

    @Test
    void a_second_attempt_is_refused_once_the_grade_was_returned() {
        ReflectionTestUtils.setField(exam, "maxAttempts", 2);
        ExamAttempt first = attempt(1, NOW.minusSeconds(1200));
        first.close(NOW.minusSeconds(600), new Grading(BigDecimal.TEN, false));
        givenTheAttempts(first);
        when(grading.isReturnedTo(exam, STUDENT)).thenReturn(true);

        assertRejected(() -> attemptService.start("luis", exam.getId()),
                "EXM_GRADE_RETURNED");
    }

    @Test
    void the_staff_does_not_take_exams() {
        when(examService.memberFor("ana", exam)).thenReturn(new CourseMember(COURSE, true,
                null));

        assertRejected(() -> attemptService.start("ana", exam.getId()),
                "EXM_ONLY_STUDENTS_TAKE");
    }

    @Test
    void an_answer_after_the_deadline_is_refused() {
        ExamAttempt late = attempt(1, NOW.minusSeconds(46 * 60));
        givenTheLocked(late);

        assertRejected(() -> attemptService.answer("luis", late.getId(), CHOICE,
                new AnswerData(List.of(RIGHT), null)), "EXM_ATTEMPT_CLOSED");
        verify(answerRepository, never()).save(any());
    }

    @Test
    void nobody_answers_on_someone_elses_attempt() {
        ExamAttempt attempt = attempt(1, NOW.minusSeconds(60));
        givenTheLocked(attempt);
        when(examService.memberFor("pedro", exam))
                .thenReturn(new CourseMember(COURSE, false, UUID.randomUUID()));

        assertRejected(() -> attemptService.answer("pedro", attempt.getId(), CHOICE,
                new AnswerData(List.of(RIGHT), null)), "EXM_ATTEMPT_OUT_OF_REACH");
    }

    @Test
    void two_options_on_a_single_choice_question_are_refused() {
        ExamAttempt attempt = attempt(1, NOW.minusSeconds(60));
        givenTheLocked(attempt);
        when(examQuestionRepository.existsByExamIdAndQuestionId(exam.getId(), CHOICE))
                .thenReturn(true);
        when(answerRepository.findByAttemptIdAndQuestionId(attempt.getId(), CHOICE))
                .thenReturn(Optional.empty());

        assertRejected(() -> attemptService.answer("luis", attempt.getId(), CHOICE,
                new AnswerData(List.of(RIGHT, WRONG), null)), "EXM_ONE_OPTION_ONLY");
    }

    @Test
    void submitting_without_open_answers_grades_and_records_the_best_grade() {
        ExamAttempt attempt = attempt(1, NOW.minusSeconds(60));
        givenTheLocked(attempt);
        answered(attempt, CHOICE, List.of(RIGHT), null);

        AttemptResponse submitted = attemptService.submit("luis", attempt.getId());

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.GRADED);
        assertThat(attempt.getScore()).isEqualByComparingTo("10");
        assertThat(attempt.getSubmittedAt()).isEqualTo(NOW);
        verify(grading).recordBestAutomatically(exam, STUDENT);
        assertThat(submitted.score()).isNull();
        assertThat(submitted.questions()).isEmpty();
    }

    @Test
    void submitting_a_written_answer_leaves_the_attempt_for_the_teacher() {
        ExamAttempt attempt = attempt(1, NOW.minusSeconds(60));
        givenTheLocked(attempt);
        answered(attempt, CHOICE, List.of(RIGHT), null);
        answered(attempt, OPEN, null, "Porque el Sol sale por el este");

        attemptService.submit("luis", attempt.getId());

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.PENDING_REVIEW);
        verify(grading, never()).recordBestAutomatically(any(), any());
    }

    @Test
    void reading_an_expired_attempt_closes_it_at_its_deadline() {
        ExamAttempt expired = attempt(1, NOW.minusSeconds(50 * 60));
        when(attemptRepository.findById(expired.getId())).thenReturn(Optional.of(expired));

        attemptService.get("luis", expired.getId());

        assertThat(expired.getStatus()).isEqualTo(AttemptStatus.GRADED);
        assertThat(expired.getSubmittedAt()).isEqualTo(expired.getDeadline());
        verify(grading).recordBestAutomatically(exam, STUDENT);
    }

    @Test
    void once_returned_the_student_sees_its_points_but_not_the_key_if_the_teacher_hides_it() {
        AttemptResponse review = reviewReturned();

        assertThat(review.score()).isEqualByComparingTo("10");
        assertThat(review.questions()).extracting(AttemptQuestionResponse::awarded)
                .containsExactly(BigDecimal.TEN, BigDecimal.ZERO);
        assertThat(review.questions()).flatExtracting(AttemptQuestionResponse::options)
                .extracting(AttemptOptionResponse::correct)
                .containsOnlyNulls();
    }

    @Test
    void once_returned_the_student_sees_the_key_if_the_teacher_shows_it() {
        ReflectionTestUtils.setField(exam, "showsAnswers", true);

        AttemptResponse review = reviewReturned();

        assertThat(review.questions().getFirst().options())
                .extracting(AttemptOptionResponse::correct)
                .containsExactly(true, false);
        assertThat(review.questions().get(1).modelAnswer()).isEqualTo("Porque sí");
    }

    private AttemptResponse reviewReturned() {
        ExamAttempt attempt = attempt(1, NOW.minusSeconds(1200));
        answered(attempt, CHOICE, List.of(RIGHT), null);
        attempt.close(NOW.minusSeconds(600), AttemptGrader.grade(
                examQuestionRepository.findByExamIdOrderByPosition(exam.getId()),
                questionService.byId(List.of()),
                Map.of(CHOICE, stored.getFirst())));
        when(attemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(grading.isReturnedTo(exam, STUDENT)).thenReturn(true);

        return attemptService.get("luis", attempt.getId());
    }

    private void assertRejected(Executable call, String code) {
        ApiException error = assertThrows(ApiException.class, call);
        assertThat(error.getCode()).isEqualTo(code);
    }

    private void givenTheAttempts(ExamAttempt... attempts) {
        when(attemptRepository.findByExamIdAndStudentIdOrderByNumber(exam.getId(), STUDENT))
                .thenReturn(List.of(attempts));
    }

    private void givenTheLocked(ExamAttempt attempt) {
        when(attemptRepository.findForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
    }

    private void answered(ExamAttempt attempt, UUID questionId, List<UUID> selected,
            String text) {
        AttemptAnswer answer = AttemptAnswer.of(attempt.getId(), questionId);
        answer.replace(selected, text);
        stored.add(answer);
    }

    private ExamAttempt attempt(int number, Instant startedAt) {
        return withId(ExamAttempt.start(exam, STUDENT, number, startedAt, 42L));
    }

    private static ExamAttempt withId(ExamAttempt attempt) {
        ReflectionTestUtils.setField(attempt, "id", UUID.randomUUID());
        return attempt;
    }
}
