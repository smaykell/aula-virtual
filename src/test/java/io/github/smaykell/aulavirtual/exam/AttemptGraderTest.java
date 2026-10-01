package io.github.smaykell.aulavirtual.exam;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.smaykell.aulavirtual.exam.question.QuestionType;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionResponse;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttemptGraderTest {

    private static final UUID EXAM = UUID.randomUUID();
    private static final UUID ATTEMPT = UUID.randomUUID();
    private static final UUID RIGHT = UUID.randomUUID();
    private static final UUID WRONG = UUID.randomUUID();
    private static final UUID ALSO_RIGHT = UUID.randomUUID();

    private final Map<UUID, QuestionResponse> questions = new HashMap<>();
    private final Map<UUID, AttemptAnswer> answers = new HashMap<>();

    @Test
    void the_right_option_earns_the_points_of_the_question() {
        UUID single = choice(QuestionType.SINGLE_CHOICE);
        answer(single, RIGHT);

        Grading grading = grade(placed(single, "8.00"));

        assertThat(grading.score()).isEqualByComparingTo("8.00");
        assertThat(grading.pendingReview()).isFalse();
    }

    @Test
    void a_wrong_option_scores_zero() {
        UUID single = choice(QuestionType.SINGLE_CHOICE);
        answer(single, WRONG);

        assertThat(grade(placed(single, "8.00")).score()).isEqualByComparingTo("0");
        assertThat(answers.get(single).getPoints()).isEqualByComparingTo("0");
    }

    @Test
    void multiple_choice_is_all_or_nothing() {
        UUID multiple = choice(QuestionType.MULTIPLE_CHOICE);
        answer(multiple, RIGHT);

        assertThat(grade(placed(multiple, "6.00")).score()).isEqualByComparingTo("0");
    }

    @Test
    void multiple_choice_with_every_right_option_earns_its_points_in_any_order() {
        UUID multiple = choice(QuestionType.MULTIPLE_CHOICE);
        answer(multiple, ALSO_RIGHT, RIGHT);

        assertThat(grade(placed(multiple, "6.00")).score()).isEqualByComparingTo("6.00");
    }

    @Test
    void an_unanswered_question_scores_zero_without_waiting_for_anyone() {
        UUID single = choice(QuestionType.SINGLE_CHOICE);
        UUID open = shortAnswer();

        Grading grading = grade(placed(single, "8.00"), placed(open, "12.00"));

        assertThat(grading.score()).isEqualByComparingTo("0");
        assertThat(grading.pendingReview()).isFalse();
    }

    @Test
    void a_written_short_answer_waits_for_the_teacher() {
        UUID single = choice(QuestionType.SINGLE_CHOICE);
        UUID open = shortAnswer();
        answer(single, RIGHT);
        AttemptAnswer written = AttemptAnswer.of(ATTEMPT, open);
        written.replace(null, "La fotosíntesis convierte luz en energía");
        answers.put(open, written);

        Grading grading = grade(placed(single, "8.00"), placed(open, "12.00"));

        assertThat(grading.pendingReview()).isTrue();
        assertThat(grading.score()).isEqualByComparingTo("8.00");
        assertThat(written.isScored()).isFalse();
    }

    @Test
    void a_short_answer_already_scored_by_the_teacher_counts_and_is_kept() {
        UUID open = shortAnswer();
        AttemptAnswer written = AttemptAnswer.of(ATTEMPT, open);
        written.replace(null, "Respuesta");
        written.award(new BigDecimal("9.50"), "Bien, falta un ejemplo");
        answers.put(open, written);

        Grading grading = grade(placed(open, "12.00"));

        assertThat(grading.pendingReview()).isFalse();
        assertThat(grading.score()).isEqualByComparingTo("9.50");
        assertThat(written.getFeedback()).isEqualTo("Bien, falta un ejemplo");
    }

    private Grading grade(ExamQuestion... placed) {
        return AttemptGrader.grade(List.of(placed), questions, answers);
    }

    private UUID choice(QuestionType type) {
        UUID id = UUID.randomUUID();
        questions.put(id, new QuestionResponse(id, ExamFixtures.COURSE, type, "Elige",
                List.of(new OptionResponse(RIGHT, "a", true),
                        new OptionResponse(WRONG, "b", false),
                        new OptionResponse(ALSO_RIGHT, "c", type == QuestionType.MULTIPLE_CHOICE)),
                null, Instant.EPOCH));
        return id;
    }

    private UUID shortAnswer() {
        UUID id = UUID.randomUUID();
        questions.put(id, new QuestionResponse(id, ExamFixtures.COURSE,
                QuestionType.SHORT_ANSWER, "Explica", List.of(), null, Instant.EPOCH));
        return id;
    }

    private void answer(UUID questionId, UUID... selected) {
        AttemptAnswer answer = AttemptAnswer.of(ATTEMPT, questionId);
        answer.replace(List.of(selected), null);
        answers.put(questionId, answer);
    }

    private static ExamQuestion placed(UUID questionId, String points) {
        return ExamQuestion.of(EXAM, questionId, 1, new BigDecimal(points));
    }
}
