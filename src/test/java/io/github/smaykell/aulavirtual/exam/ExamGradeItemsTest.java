package io.github.smaykell.aulavirtual.exam;

import static io.github.smaykell.aulavirtual.exam.ExamFixtures.COURSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamGradeItemsTest {

    @Mock
    private ExamRepository examRepository;

    @Test
    void an_exam_with_questions_is_a_column_of_the_gradebook_due_when_it_closes() {
        Exam exam = ExamFixtures.exam();
        exam.scoreOutOf(new BigDecimal("20.00"));
        when(examRepository.findByCourseIdAndMaxScoreGreaterThanOrderByClosesAt(COURSE,
                BigDecimal.ZERO)).thenReturn(List.of(exam));

        List<GradeItem> items = new ExamGradeItems(examRepository).itemsOf(COURSE);

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.sourceType()).isEqualTo(GradeSource.EXAM);
            assertThat(item.sourceId()).isEqualTo(exam.getId());
            assertThat(item.maxScore()).isEqualByComparingTo("20.00");
            assertThat(item.dueAt()).isEqualTo(ExamFixtures.CLOSES);
        });
    }
}
