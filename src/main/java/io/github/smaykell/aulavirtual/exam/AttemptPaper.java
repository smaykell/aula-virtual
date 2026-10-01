package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.exam.dto.AttemptOptionResponse;
import io.github.smaykell.aulavirtual.exam.dto.AttemptQuestionResponse;
import io.github.smaykell.aulavirtual.exam.dto.AttemptResponse;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionResponse;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

record AttemptPaper(Exam exam, ExamAttempt attempt, List<ExamQuestion> placed,
        Map<UUID, QuestionResponse> questions, Map<UUID, AttemptAnswer> answers) {

    AttemptResponse disclosed(Disclosure disclosure) {
        return new AttemptResponse(attempt.getId(), attempt.getExamId(), attempt.getStudentId(),
                attempt.getNumber(), attempt.getStatus(), attempt.getStartedAt(),
                attempt.getDeadline(), attempt.getSubmittedAt(),
                disclosure.showsScores() ? attempt.getScore() : null,
                disclosure.showsQuestions() ? questionsFor(disclosure) : List.of());
    }

    private List<AttemptQuestionResponse> questionsFor(Disclosure disclosure) {
        List<ExamQuestion> order = new ArrayList<>(placed);
        if (exam.isShuffleQuestions()) {
            Collections.shuffle(order, new Random(attempt.getSeed()));
        }
        return order.stream().map(question -> questionFor(question, disclosure)).toList();
    }

    private AttemptQuestionResponse questionFor(ExamQuestion placedQuestion,
            Disclosure disclosure) {

        QuestionResponse question = questions.get(placedQuestion.getQuestionId());
        AttemptAnswer answer = answers.get(question.id());
        return new AttemptQuestionResponse(question.id(), placedQuestion.getPoints(),
                question.type(), question.statement(), optionsFor(question, disclosure),
                answer == null ? List.of() : answer.selectedOrNone(),
                answer == null ? null : answer.getText(),
                disclosure.showsScores() ? awarded(answer) : null,
                disclosure.showsScores() && answer != null ? answer.getFeedback() : null,
                disclosure.showsKey() ? question.modelAnswer() : null);
    }

    private List<AttemptOptionResponse> optionsFor(QuestionResponse question,
            Disclosure disclosure) {

        List<OptionResponse> options = new ArrayList<>(question.options());
        if (exam.isShuffleOptions()) {
            Collections.shuffle(options,
                    new Random(attempt.getSeed() ^ question.id().getLeastSignificantBits()));
        }
        return options.stream()
                .map(option -> new AttemptOptionResponse(option.id(), option.text(),
                        disclosure.showsKey() ? option.correct() : null))
                .toList();
    }

    private BigDecimal awarded(AttemptAnswer answer) {
        if (answer != null) {
            return answer.getPoints();
        }
        return attempt.isInProgress() ? null : BigDecimal.ZERO;
    }
}
