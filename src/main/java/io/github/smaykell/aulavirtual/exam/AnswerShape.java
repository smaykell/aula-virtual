package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.exam.dto.AnswerData;
import io.github.smaykell.aulavirtual.exam.exception.ForeignOptionException;
import io.github.smaykell.aulavirtual.exam.exception.OneOptionOnlyException;
import io.github.smaykell.aulavirtual.exam.question.QuestionType;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionResponse;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class AnswerShape {

    private AnswerShape() {
    }

    static void fill(AttemptAnswer answer, QuestionResponse question, AnswerData data) {
        if (question.type() == QuestionType.SHORT_ANSWER) {
            answer.replace(null, trimmed(data.text()));
        } else {
            answer.replace(choicesOf(question, data.selectedOrNone()), null);
        }
    }

    private static List<UUID> choicesOf(QuestionResponse question, List<UUID> selected) {
        List<UUID> distinct = selected.stream().distinct().toList();
        Set<UUID> offered = question.options().stream()
                .map(OptionResponse::id)
                .collect(Collectors.toSet());
        if (!offered.containsAll(distinct)) {
            throw new ForeignOptionException();
        }
        if (question.type() != QuestionType.MULTIPLE_CHOICE && distinct.size() > 1) {
            throw new OneOptionOnlyException();
        }
        return distinct;
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
