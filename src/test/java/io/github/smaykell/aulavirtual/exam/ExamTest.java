package io.github.smaykell.aulavirtual.exam;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.smaykell.aulavirtual.exam.dto.ExamData;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExamTest {

    @Test
    void the_time_limit_counts_from_the_moment_the_student_starts() {
        Exam exam = ExamFixtures.exam();
        Instant start = ExamFixtures.OPENS.plusSeconds(600);

        assertThat(exam.deadlineFor(start)).isEqualTo(start.plusSeconds(45 * 60));
    }

    @Test
    void whoever_starts_late_only_has_until_the_exam_closes() {
        Exam exam = ExamFixtures.exam();
        Instant lateStart = ExamFixtures.CLOSES.minusSeconds(600);

        assertThat(exam.deadlineFor(lateStart)).isEqualTo(ExamFixtures.CLOSES);
    }

    @Test
    void without_a_time_limit_everyone_finishes_when_the_exam_closes() {
        ExamData data = ExamFixtures.data();
        Exam exam = Exam.create(ExamFixtures.UNIT, ExamFixtures.COURSE, new ExamData(
                data.title(), null, data.opensAt(), data.closesAt(), null, 1, false, false,
                false, null));

        assertThat(exam.deadlineFor(ExamFixtures.OPENS)).isEqualTo(ExamFixtures.CLOSES);
    }

    @Test
    void the_exam_is_open_from_its_opening_up_to_but_not_including_its_closing() {
        Exam exam = ExamFixtures.exam();

        assertThat(exam.isOpenAt(ExamFixtures.OPENS.minusSeconds(1))).isFalse();
        assertThat(exam.isOpenAt(ExamFixtures.OPENS)).isTrue();
        assertThat(exam.isOpenAt(ExamFixtures.CLOSES.minusSeconds(1))).isTrue();
        assertThat(exam.isOpenAt(ExamFixtures.CLOSES)).isFalse();
    }
}
