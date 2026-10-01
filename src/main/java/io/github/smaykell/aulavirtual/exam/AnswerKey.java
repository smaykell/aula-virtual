package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.exam.exception.AnswersRequireStaffException;

public final class AnswerKey {

    private AnswerKey() {
    }

    public static void requireReadableBy(CourseMember member) {
        if (!member.staff()) {
            throw new AnswersRequireStaffException();
        }
    }
}
