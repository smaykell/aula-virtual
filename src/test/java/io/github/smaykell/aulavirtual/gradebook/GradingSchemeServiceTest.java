package io.github.smaykell.aulavirtual.gradebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.gradebook.dto.CategoryData;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeCategoryResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeData;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GradingSchemeServiceTest {

    private static final UUID COURSE = UUID.randomUUID();
    private static final BigDecimal PASSING = new BigDecimal("13.00");

    @Mock
    private GradingSchemeRepository schemeRepository;

    @Mock
    private GradeCategoryRepository categoryRepository;

    @Mock
    private CourseService courseService;

    private GradingSchemeService schemeService;

    @BeforeEach
    void setUp() {
        schemeService = new GradingSchemeService(schemeRepository, categoryRepository,
                courseService);
    }

    @Test
    void a_course_that_never_set_its_scheme_adds_points_and_passes_with_thirteen() {
        when(courseService.memberOf("ana.estudiante", COURSE))
                .thenReturn(new CourseMember(COURSE, false, UUID.randomUUID()));
        when(schemeRepository.findByCourseId(COURSE)).thenReturn(Optional.empty());
        when(categoryRepository.findByCourseIdOrderByPosition(COURSE)).thenReturn(List.of());

        GradingSchemeResponse scheme = schemeService.get("ana.estudiante", COURSE);

        assertThat(scheme.method()).isEqualTo(GradingMethod.TOTAL_POINTS);
        assertThat(scheme.passingScore()).isEqualTo(PASSING);
        assertThat(scheme.categories()).isEmpty();
    }

    @Test
    void weighting_the_course_creates_its_categories_in_the_order_given() {
        givenTheCurrentCategories();
        givenCategoriesAreStored();

        GradingSchemeResponse scheme = schemeService.replace("juan", COURSE, weighted(
                category(null, "Tareas", "40"), category(null, "Examenes", "60")));

        assertThat(scheme.method()).isEqualTo(GradingMethod.WEIGHTED);
        assertThat(scheme.categories())
                .extracting(GradeCategoryResponse::name, GradeCategoryResponse::position)
                .containsExactly(tuple("Tareas", 1), tuple("Examenes", 2));
        verify(schemeRepository).save(any(GradingScheme.class));
    }

    @Test
    void weights_that_do_not_add_up_to_a_hundred_are_rejected() {
        ApiException error = assertThrows(ApiException.class,
                () -> schemeService.replace("juan", COURSE, weighted(
                        category(null, "Tareas", "40"), category(null, "Examenes", "50"))));

        assertThat(error.getCode()).isEqualTo("GRB_WEIGHTS_DO_NOT_ADD_UP");
        assertThat(error.getMessage()).contains("90");
        verify(schemeRepository, never()).save(any(GradingScheme.class));
    }

    @Test
    void a_weighted_scheme_without_categories_is_rejected() {
        ApiException error = assertThrows(ApiException.class,
                () -> schemeService.replace("juan", COURSE, weighted()));

        assertThat(error.getCode()).isEqualTo("GRB_WEIGHTS_DO_NOT_ADD_UP");
    }

    @Test
    void total_points_does_not_care_how_the_weights_add_up() {
        givenTheCurrentCategories();
        givenCategoriesAreStored();

        GradingSchemeResponse scheme = schemeService.replace("juan", COURSE,
                new GradingSchemeData(GradingMethod.TOTAL_POINTS, PASSING,
                        List.of(category(null, "Tareas", "0"))));

        assertThat(scheme.method()).isEqualTo(GradingMethod.TOTAL_POINTS);
        assertThat(scheme.categories()).hasSize(1);
    }

    @Test
    void two_categories_with_the_same_name_regardless_of_case_are_rejected() {
        ApiException error = assertThrows(ApiException.class,
                () -> schemeService.replace("juan", COURSE, weighted(
                        category(null, "Tareas", "50"), category(null, " tareas ", "50"))));

        assertThat(error.getCode()).isEqualTo("GRB_DUPLICATE_CATEGORY_NAME");
    }

    @Test
    void replacing_the_scheme_renames_what_it_keeps_and_drops_what_it_leaves_out() {
        GradeCategory kept = existing("Tareas", "50", 1);
        GradeCategory dropped = existing("Participacion", "50", 2);
        givenTheCurrentCategories(kept, dropped);

        GradingSchemeResponse scheme = schemeService.replace("juan", COURSE,
                weighted(category(kept.getId(), "Trabajos", "100")));

        assertThat(kept.getName()).isEqualTo("Trabajos");
        assertThat(kept.getWeight()).isEqualTo(new BigDecimal("100"));
        assertThat(scheme.categories()).extracting(GradeCategoryResponse::id)
                .containsExactly(kept.getId());
        verify(categoryRepository).deleteAll(List.of(dropped));
    }

    @Test
    void a_category_of_another_course_cannot_be_kept() {
        givenTheCurrentCategories();
        UUID foreign = UUID.randomUUID();

        ApiException error = assertThrows(ApiException.class,
                () -> schemeService.replace("juan", COURSE,
                        weighted(category(foreign, "Tareas", "100"))));

        assertThat(error.getCode()).isEqualTo("GRB_CATEGORY_NOT_FOUND");
    }

    @Test
    void the_same_category_twice_is_rejected() {
        GradeCategory kept = existing("Tareas", "100", 1);
        givenTheCurrentCategories(kept);

        ApiException error = assertThrows(ApiException.class,
                () -> schemeService.replace("juan", COURSE, weighted(
                        category(kept.getId(), "Tareas", "50"),
                        category(kept.getId(), "Trabajos", "50"))));

        assertThat(error.getCode()).isEqualTo("GRB_REPEATED_CATEGORY");
    }

    @Test
    void an_archived_course_keeps_its_scheme() {
        doThrow(new ArchivedCourseException()).when(courseService)
                .requireWritable("juan", COURSE);

        ApiException error = assertThrows(ApiException.class,
                () -> schemeService.replace("juan", COURSE,
                        weighted(category(null, "Tareas", "100"))));

        assertThat(error.getCode()).isEqualTo("CRS_ARCHIVED");
    }

    @Test
    void a_category_outside_the_course_is_not_found() {
        UUID category = UUID.randomUUID();
        when(categoryRepository.existsByIdAndCourseId(category, COURSE)).thenReturn(false);

        ApiException error = assertThrows(ApiException.class,
                () -> schemeService.requireCategoryIn(COURSE, category));

        assertThat(error.getCode()).isEqualTo("GRB_CATEGORY_NOT_FOUND");
    }

    private void givenTheCurrentCategories(GradeCategory... categories) {
        when(schemeRepository.findByCourseId(COURSE)).thenReturn(Optional.empty());
        when(categoryRepository.findByCourseIdOrderByPosition(COURSE))
                .thenReturn(List.of(categories));
    }

    private void givenCategoriesAreStored() {
        when(categoryRepository.save(any(GradeCategory.class))).thenAnswer(call -> {
            GradeCategory category = call.getArgument(0);
            ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
            return category;
        });
    }

    private static GradeCategory existing(String name, String weight, int position) {
        GradeCategory category = GradeCategory.create(COURSE, name, new BigDecimal(weight),
                position);
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        return category;
    }

    private static GradingSchemeData weighted(CategoryData... categories) {
        return new GradingSchemeData(GradingMethod.WEIGHTED, PASSING, List.of(categories));
    }

    private static CategoryData category(UUID id, String name, String weight) {
        return new CategoryData(id, name, new BigDecimal(weight));
    }
}
