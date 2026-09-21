package io.github.smaykell.aulavirtual.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CourseAccessBoundaryTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final Path MODULE = SOURCES.resolve(
            Path.of("io", "github", "smaykell", "aulavirtual", "course"));
    private static final Path GATE = MODULE.resolve("CourseAccess.java");
    private static final String GATE_NAME = "CourseAccess";

    @Test
    void only_the_course_module_goes_through_CourseAccess() throws IOException {
        List<Path> trespassers = javaFilesOutsideTheModule().stream()
                .filter(CourseAccessBoundaryTest::mentionsTheGate)
                .toList();

        assertThat(trespassers)
                .describedAs("CourseAccess es publico solo porque lo comparten los "
                        + "sub-paquetes de course; fuera del modulo se habla con "
                        + "CourseService o UnitService")
                .isEmpty();
    }

    @Test
    void the_gate_is_where_this_test_believes_it_is() {
        assertThat(GATE).exists();
    }

    @Test
    void the_scan_reaches_the_sources_of_every_module() throws IOException {
        assertThat(javaFilesOutsideTheModule())
                .map(Path::getFileName)
                .map(Path::toString)
                .contains("AulaVirtualApplication.java", "SubmissionService.java");
    }

    private static List<Path> javaFilesOutsideTheModule() throws IOException {
        try (Stream<Path> tree = Files.walk(SOURCES)) {
            return tree.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(MODULE))
                    .toList();
        }
    }

    private static boolean mentionsTheGate(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).contains(GATE_NAME);
        } catch (IOException cannotRead) {
            throw new IllegalStateException("No se pudo leer " + path, cannotRead);
        }
    }
}
