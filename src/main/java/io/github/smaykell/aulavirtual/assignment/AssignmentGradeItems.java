package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.gradebook.GradeItemProvider;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AssignmentGradeItems implements GradeItemProvider {

    private final AssignmentRepository assignmentRepository;

    @Override
    public List<GradeItem> itemsOf(UUID courseId) {
        return assignmentRepository.findByCourseIdOrderByDueAt(courseId).stream()
                .map(AssignmentGradeItems::itemOf)
                .toList();
    }

    private static GradeItem itemOf(Assignment assignment) {
        return new GradeItem(GradeSource.ASSIGNMENT, assignment.getId(), assignment.getTitle(),
                assignment.getCategoryId(), assignment.getMaxScore(), assignment.getDueAt());
    }
}
