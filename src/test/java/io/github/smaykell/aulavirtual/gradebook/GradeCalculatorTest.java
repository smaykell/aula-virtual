package io.github.smaykell.aulavirtual.gradebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.smaykell.aulavirtual.gradebook.dto.CategoryAverage;
import io.github.smaykell.aulavirtual.gradebook.dto.FinalGrade;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeCategoryResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradeCalculatorTest {

    private static final UUID TASKS = UUID.randomUUID();
    private static final UUID EXAMS = UUID.randomUUID();
    private static final UUID PARTICIPATION = UUID.randomUUID();
    private static final BigDecimal PASSING = new BigDecimal("13.00");

    @Test
    void total_points_adds_every_score_and_every_maximum_before_scaling_to_twenty() {
        FinalGrade grade = finalGradeOf(totalPoints(),
                mark(TASKS, "15", "20"), mark(EXAMS, "8", "10"));

        assertThat(grade.score()).isEqualByComparingTo("15.33");
        assertThat(grade.roundedScore()).isEqualTo(15);
        assertThat(grade.passed()).isTrue();
    }

    @Test
    void total_points_counts_the_tasks_without_a_category_too() {
        FinalGrade grade = finalGradeOf(totalPoints(),
                mark(TASKS, "20", "20"), mark(null, "0", "20"));

        assertThat(grade.score()).isEqualByComparingTo("10.00");
    }

    @Test
    void weighted_averages_each_category_and_weighs_the_averages() {
        FinalGrade grade = finalGradeOf(weighted(),
                mark(TASKS, "18", "20"), mark(EXAMS, "10", "20"));

        assertThat(grade.score()).isEqualByComparingTo("13.20");
        assertThat(grade.roundedScore()).isEqualTo(13);
        assertThat(grade.passed()).isTrue();
    }

    @Test
    void inside_a_category_a_task_weighs_by_its_points() {
        FinalGrade grade = finalGradeOf(weighted(),
                mark(TASKS, "10", "10"), mark(TASKS, "0", "30"), mark(EXAMS, "10", "20"));

        assertThat(grade.score()).isEqualByComparingTo("8.00");
    }

    @Test
    void a_category_still_without_grades_leaves_its_weight_to_the_others() {
        FinalGrade grade = finalGradeOf(weighted(), mark(TASKS, "18", "20"));

        assertThat(grade.score()).isEqualByComparingTo("18.00");
    }

    @Test
    void weighted_ignores_the_tasks_without_a_category() {
        FinalGrade grade = finalGradeOf(weighted(),
                mark(TASKS, "20", "20"), mark(null, "0", "20"));

        assertThat(grade.score()).isEqualByComparingTo("20.00");
    }

    @Test
    void a_category_weighing_nothing_does_not_move_the_final_grade() {
        FinalGrade grade = finalGradeOf(weighted(),
                mark(TASKS, "20", "20"), mark(PARTICIPATION, "0", "20"));

        assertThat(grade.score()).isEqualByComparingTo("20.00");
    }

    @Test
    void with_nothing_graded_there_is_no_final_grade() {
        assertThat(GradeCalculator.finalGrade(weighted(), List.of())).isEmpty();
        assertThat(GradeCalculator.finalGrade(totalPoints(), List.of())).isEmpty();
    }

    @Test
    void only_uncategorised_grades_leave_a_weighted_course_without_final_grade() {
        assertThat(GradeCalculator.finalGrade(weighted(), List.of(mark(null, "20", "20"))))
                .isEmpty();
    }

    @Test
    void half_a_point_rounds_up_and_passes() {
        FinalGrade grade = finalGradeOf(totalPoints(), mark(TASKS, "12.50", "20"));

        assertThat(grade.roundedScore()).isEqualTo(13);
        assertThat(grade.passed()).isTrue();
    }

    @Test
    void just_under_half_a_point_rounds_down_and_fails() {
        FinalGrade grade = finalGradeOf(totalPoints(), mark(TASKS, "12.49", "20"));

        assertThat(grade.roundedScore()).isEqualTo(12);
        assertThat(grade.passed()).isFalse();
    }

    @Test
    void the_rounded_grade_follows_the_two_decimals_the_student_sees() {
        FinalGrade grade = finalGradeOf(totalPoints(), mark(TASKS, "24.99", "40"));

        assertThat(grade.score()).isEqualByComparingTo("12.50");
        assertThat(grade.roundedScore()).isEqualTo(13);
    }

    @Test
    void the_passing_score_of_the_course_decides_who_passes() {
        GradingSchemeResponse demanding = new GradingSchemeResponse(GradingMethod.TOTAL_POINTS,
                new BigDecimal("15.00"), List.of());

        FinalGrade grade = finalGradeOf(demanding, mark(null, "14", "20"));

        assertThat(grade.passed()).isFalse();
    }

    @Test
    void each_category_with_grades_gets_its_average_on_the_scale_of_twenty() {
        List<CategoryAverage> averages = GradeCalculator.categoryAverages(weighted(), List.of(
                mark(TASKS, "7", "10"), mark(TASKS, "20", "20"), mark(null, "0", "20")));

        assertThat(averages).extracting(CategoryAverage::categoryId, CategoryAverage::score)
                .containsExactly(tuple(TASKS, new BigDecimal("18.00")));
    }

    private static FinalGrade finalGradeOf(GradingSchemeResponse scheme, Mark... marks) {
        return GradeCalculator.finalGrade(scheme, List.of(marks)).orElseThrow();
    }

    private static GradingSchemeResponse weighted() {
        return new GradingSchemeResponse(GradingMethod.WEIGHTED, PASSING, List.of(
                category(TASKS, "Tareas", "40", 1),
                category(EXAMS, "Examenes", "60", 2),
                category(PARTICIPATION, "Participacion", "0", 3)));
    }

    private static GradingSchemeResponse totalPoints() {
        return new GradingSchemeResponse(GradingMethod.TOTAL_POINTS, PASSING, List.of(
                category(TASKS, "Tareas", "0", 1), category(EXAMS, "Examenes", "0", 2)));
    }

    private static GradeCategoryResponse category(UUID id, String name, String weight,
            int position) {

        return new GradeCategoryResponse(id, name, new BigDecimal(weight), position);
    }

    private static Mark mark(UUID category, String score, String maxScore) {
        return new Mark(category, new BigDecimal(score), new BigDecimal(maxScore));
    }
}
