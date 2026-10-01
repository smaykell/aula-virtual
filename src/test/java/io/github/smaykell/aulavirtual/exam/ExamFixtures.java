package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.exam.dto.ExamData;
import java.time.Instant;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

final class ExamFixtures {

    static final UUID UNIT = UUID.randomUUID();
    static final UUID COURSE = UUID.randomUUID();
    static final Instant OPENS = Instant.parse("2026-10-15T13:00:00Z");
    static final Instant CLOSES = Instant.parse("2026-10-15T15:00:00Z");

    private ExamFixtures() {
    }

    static ExamData data() {
        return window(OPENS, CLOSES);
    }

    static ExamData window(Instant opensAt, Instant closesAt) {
        return new ExamData("Primer parcial", "Lee con calma", opensAt, closesAt, 45, 1, false,
                false, false, null);
    }

    static Exam exam() {
        Exam exam = Exam.create(UNIT, COURSE, data());
        ReflectionTestUtils.setField(exam, "id", UUID.randomUUID());
        return exam;
    }
}
