package io.github.smaykell.aulavirtual.exam;

enum Disclosure {

    PAPER(true, false, false),
    HIDDEN(false, false, false),
    SCORES(true, true, false),
    ANSWERS(true, true, true);

    private final boolean questions;
    private final boolean scores;
    private final boolean key;

    Disclosure(boolean questions, boolean scores, boolean key) {
        this.questions = questions;
        this.scores = scores;
        this.key = key;
    }

    boolean showsQuestions() {
        return questions;
    }

    boolean showsScores() {
        return scores;
    }

    boolean showsKey() {
        return key;
    }
}
