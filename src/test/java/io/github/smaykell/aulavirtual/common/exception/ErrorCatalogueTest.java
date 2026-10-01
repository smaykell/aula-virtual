package io.github.smaykell.aulavirtual.common.exception;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.smaykell.aulavirtual.administrator.exception.AdministratorError;
import io.github.smaykell.aulavirtual.assignment.exception.AssignmentError;
import io.github.smaykell.aulavirtual.course.exception.CourseError;
import io.github.smaykell.aulavirtual.exam.exception.ExamError;
import io.github.smaykell.aulavirtual.gradebook.exception.GradebookError;
import io.github.smaykell.aulavirtual.person.exception.PersonError;
import io.github.smaykell.aulavirtual.settings.exception.SettingsError;
import io.github.smaykell.aulavirtual.student.exception.StudentError;
import io.github.smaykell.aulavirtual.teacher.exception.TeacherError;
import io.github.smaykell.aulavirtual.user.exception.UserError;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

class ErrorCatalogueTest {

    private static final String ROOT_PACKAGE = "io.github.smaykell.aulavirtual";
    private static final List<Class<?>> CATALOGUES = scanForCatalogues();
    private static final List<ErrorCode> ERRORS = CATALOGUES.stream()
            .flatMap(catalogue -> Arrays.stream(catalogue.getEnumConstants()))
            .map(ErrorCode.class::cast)
            .toList();

    @Test
    void the_scan_finds_the_catalogue_of_every_module() {
        assertThat(CATALOGUES)
                .containsExactlyInAnyOrder(CommonError.class, PersonError.class, UserError.class,
                        TeacherError.class, AdministratorError.class, CourseError.class,
                        StudentError.class, AssignmentError.class, SettingsError.class,
                        GradebookError.class, ExamError.class);
    }

    @Test
    void the_base_exception_cannot_be_thrown_without_a_name() {
        assertThat(ApiException.class).isAbstract();
    }

    @Test
    void no_two_errors_share_a_code() {
        assertThat(ERRORS.stream().map(ErrorCode::code).toList()).doesNotHaveDuplicates();
    }

    @Test
    void every_code_is_its_prefix_plus_its_name() {
        assertThat(ERRORS).allSatisfy(error ->
                assertThat(error.code()).isEqualTo(error.prefix() + "_" + error.name()));
    }

    @Test
    void every_catalogue_uses_a_prefix_of_its_own() {
        Set<String> prefixes = ERRORS.stream().map(ErrorCode::prefix).collect(toSet());

        assertThat(prefixes).hasSameSizeAs(CATALOGUES);
    }

    @Test
    void every_error_carries_a_status_and_a_message() {
        assertThat(ERRORS).allSatisfy(error -> {
            assertThat(error.status()).isNotNull();
            assertThat(error.message()).isNotBlank();
        });
    }

    @Test
    void a_message_with_a_placeholder_is_filled_by_format() {
        assertThat(CommonError.PARAMETER_TYPE_MISMATCH.format("id"))
                .isEqualTo("El parámetro id no tiene un formato válido");
    }

    @Test
    void a_message_without_arguments_is_left_untouched() {
        assertThat(CommonError.UNEXPECTED.format()).isEqualTo(CommonError.UNEXPECTED.message());
    }

    private static List<Class<?>> scanForCatalogues() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));

        return scanner.findCandidateComponents(ROOT_PACKAGE).stream()
                .<Class<?>>map(definition -> forName(definition.getBeanClassName()))
                .toList();
    }

    private static Class<?> forName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(className, ex);
        }
    }
}
