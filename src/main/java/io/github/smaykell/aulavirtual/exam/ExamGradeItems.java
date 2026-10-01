package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.gradebook.GradeItemProvider;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ExamGradeItems implements GradeItemProvider {

    private final ExamRepository examRepository;

    @Override
    public List<GradeItem> itemsOf(UUID courseId) {
        return examRepository
                .findByCourseIdAndMaxScoreGreaterThanOrderByClosesAt(courseId, BigDecimal.ZERO)
                .stream()
                .map(ExamGradeItems::itemOf)
                .toList();
    }

    private static GradeItem itemOf(Exam exam) {
        return new GradeItem(GradeSource.EXAM, exam.getId(), exam.getTitle(),
                exam.getCategoryId(), exam.getMaxScore(), exam.getClosesAt());
    }
}
