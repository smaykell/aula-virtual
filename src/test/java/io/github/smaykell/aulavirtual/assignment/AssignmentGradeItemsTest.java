package io.github.smaykell.aulavirtual.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentGradeItemsTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Test
    void every_task_of_the_course_is_a_column_of_its_gradebook() {
        UUID category = UUID.randomUUID();
        Assignment assignment = AssignmentFixtures.assignment();
        assignment.update(AssignmentFixtures.data(AssignmentFixtures.DUE_AT, false, category));
        when(assignmentRepository.findByCourseIdOrderByDueAt(AssignmentFixtures.COURSE))
                .thenReturn(List.of(assignment));

        List<GradeItem> items = new AssignmentGradeItems(assignmentRepository)
                .itemsOf(AssignmentFixtures.COURSE);

        assertThat(items).containsExactly(new GradeItem(GradeSource.ASSIGNMENT,
                assignment.getId(), "Practica 1", category, AssignmentFixtures.MAX_SCORE,
                AssignmentFixtures.DUE_AT));
    }
}
