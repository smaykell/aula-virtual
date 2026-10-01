package io.github.smaykell.aulavirtual.exam.question;

public enum QuestionType {

    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER;

    public boolean gradedAutomatically() {
        return this != SHORT_ANSWER;
    }
}
