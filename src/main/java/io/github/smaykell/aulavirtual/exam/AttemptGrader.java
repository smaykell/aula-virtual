package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.exam.question.QuestionType;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionResponse;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class AttemptGrader {

    private AttemptGrader() {
    }

    static Grading grade(List<ExamQuestion> placed, Map<UUID, QuestionResponse> questions,
            Map<UUID, AttemptAnswer> answers) {

        BigDecimal score = BigDecimal.ZERO;
        boolean pendingReview = false;
        for (ExamQuestion question : placed) {
            AttemptAnswer answer = answers.get(question.getQuestionId());
            if (answer == null) {
                continue;
            }
            if (!answer.isScored()) {
                scoreAutomatically(answer, questions.get(question.getQuestionId()),
                        question.getPoints());
            }
            if (answer.isScored()) {
                score = score.add(answer.getPoints());
            } else {
                pendingReview = true;
            }
        }
        return new Grading(score, pendingReview);
    }

    private static void scoreAutomatically(AttemptAnswer answer, QuestionResponse question,
            BigDecimal points) {

        if (answer.isBlank()) {
            answer.award(BigDecimal.ZERO, null);
        } else if (question.type() != QuestionType.SHORT_ANSWER) {
            answer.award(isRight(answer, question) ? points : BigDecimal.ZERO, null);
        }
    }

    private static boolean isRight(AttemptAnswer answer, QuestionResponse question) {
        Set<UUID> correct = question.options().stream()
                .filter(OptionResponse::correct)
                .map(OptionResponse::id)
                .collect(Collectors.toSet());
        return new HashSet<>(answer.selectedOrNone()).equals(correct);
    }
}
