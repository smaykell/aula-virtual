package io.github.smaykell.aulavirtual.exam.question;

import io.github.smaykell.aulavirtual.exam.dto.ExamConstraints;
import io.github.smaykell.aulavirtual.exam.exception.ChoicesNotAllowedException;
import io.github.smaykell.aulavirtual.exam.exception.ChoicesOutOfRangeException;
import io.github.smaykell.aulavirtual.exam.exception.OneCorrectChoiceException;
import io.github.smaykell.aulavirtual.exam.exception.SomeCorrectChoiceException;
import io.github.smaykell.aulavirtual.exam.exception.TruthRequiredException;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionData;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionData;
import java.util.List;

final class QuestionShape {

    private static final String TRUE = "Verdadero";
    private static final String FALSE = "Falso";

    private QuestionShape() {
    }

    static List<OptionData> optionsFor(QuestionData data) {
        return switch (data.type()) {
            case SINGLE_CHOICE -> singleChoice(data.optionsOrNone());
            case MULTIPLE_CHOICE -> multipleChoice(data.optionsOrNone());
            case TRUE_FALSE -> trueOrFalse(data);
            case SHORT_ANSWER -> withoutChoices(data);
        };
    }

    private static List<OptionData> singleChoice(List<OptionData> options) {
        requireChoiceCount(options);
        if (correctAmong(options) != 1) {
            throw new OneCorrectChoiceException();
        }
        return options;
    }

    private static List<OptionData> multipleChoice(List<OptionData> options) {
        requireChoiceCount(options);
        if (correctAmong(options) == 0) {
            throw new SomeCorrectChoiceException();
        }
        return options;
    }

    private static List<OptionData> trueOrFalse(QuestionData data) {
        withoutChoices(data);
        if (data.statementIsTrue() == null) {
            throw new TruthRequiredException();
        }
        return List.of(new OptionData(TRUE, data.statementIsTrue()),
                new OptionData(FALSE, !data.statementIsTrue()));
    }

    private static List<OptionData> withoutChoices(QuestionData data) {
        if (!data.optionsOrNone().isEmpty()) {
            throw new ChoicesNotAllowedException();
        }
        return List.of();
    }

    private static void requireChoiceCount(List<OptionData> options) {
        if (options.size() < ExamConstraints.CHOICES_MIN
                || options.size() > ExamConstraints.CHOICES_MAX) {
            throw new ChoicesOutOfRangeException(ExamConstraints.CHOICES_MIN,
                    ExamConstraints.CHOICES_MAX);
        }
    }

    private static long correctAmong(List<OptionData> options) {
        return options.stream().filter(OptionData::correct).count();
    }
}
