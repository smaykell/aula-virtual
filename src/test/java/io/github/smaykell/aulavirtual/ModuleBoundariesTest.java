package io.github.smaykell.aulavirtual;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModuleBoundariesTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final String BASE = "io.github.smaykell.aulavirtual.";
    private static final Set<String> CROSSCUTTING = Set.of("common", "config", "security");

    @Test
    void no_module_reaches_into_the_repository_of_another() throws IOException {
        assertThat(crossingsInto(repositories())).isEmpty();
    }

    @Test
    void no_module_reaches_into_the_entity_of_another() throws IOException {
        assertThat(crossingsInto(entities())).isEmpty();
    }

    @Test
    void the_scan_finds_the_repository_of_every_module() throws IOException {
        assertThat(simpleNamesOf(repositories())).containsExactlyInAnyOrder(
                "AdministratorRepository", "AssignmentRepository", "CourseRepository",
                "EnrollmentRepository", "GradeRepository", "MaterialRepository",
                "PersonRepository", "StudentRepository", "SubmissionRepository",
                "TeacherRepository", "UnitRepository", "UserRepository");
    }

    @Test
    void the_scan_finds_the_entity_of_every_module() throws IOException {
        assertThat(simpleNamesOf(entities())).containsExactlyInAnyOrder(
                "Administrator", "Assignment", "Course", "Enrollment", "Grade", "Material",
                "Person", "Student", "Submission", "Teacher", "Unit", "User");
    }

    @Test
    void the_shared_base_class_of_the_entities_is_not_taken_for_one() throws IOException {
        assertThat(entities()).doesNotContain(BASE + "common.domain.BaseEntity");
    }

    private static List<String> crossingsInto(List<String> internals) throws IOException {
        List<String> crossings = new ArrayList<>();
        for (Path file : javaFiles()) {
            String name = fullyQualifiedNameOf(file);
            String module = moduleOf(name);
            if (module == null) {
                continue;
            }
            importsOf(file).stream()
                    .filter(internals::contains)
                    .filter(imported -> isAnotherModule(module, imported))
                    .forEach(imported -> crossings.add(name + " -> " + imported));
        }
        return crossings;
    }

    private static boolean isAnotherModule(String module, String imported) {
        String owner = moduleOf(imported);
        return owner != null && !owner.equals(module);
    }

    private static String moduleOf(String fullyQualifiedName) {
        if (!fullyQualifiedName.startsWith(BASE)) {
            return null;
        }
        String withinBase = fullyQualifiedName.substring(BASE.length());
        int firstDot = withinBase.indexOf('.');
        if (firstDot < 0) {
            return null;
        }
        String module = withinBase.substring(0, firstDot);
        return CROSSCUTTING.contains(module) ? null : module;
    }

    private static List<String> entities() throws IOException {
        return sourcesWhere(ModuleBoundariesTest::declaresAnEntity);
    }

    private static List<String> repositories() throws IOException {
        return sourcesWhere(source -> source.contains("JpaRepository"));
    }

    private static boolean declaresAnEntity(String source) {
        return source.lines()
                .anyMatch(line -> line.equals("@Entity") || line.startsWith("@Entity("));
    }

    private static List<String> sourcesWhere(Predicate<String> matches) throws IOException {
        List<String> found = new ArrayList<>();
        for (Path file : javaFiles()) {
            if (matches.test(contentOf(file))) {
                found.add(fullyQualifiedNameOf(file));
            }
        }
        return found;
    }

    private static List<String> importsOf(Path file) {
        return contentOf(file).lines()
                .filter(line -> line.startsWith("import ") && !line.startsWith("import static "))
                .map(line -> line.substring("import ".length()).replace(";", "").trim())
                .toList();
    }

    private static List<String> simpleNamesOf(List<String> fullyQualifiedNames) {
        return fullyQualifiedNames.stream()
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .toList();
    }

    private static String fullyQualifiedNameOf(Path file) {
        String path = SOURCES.relativize(file).toString();
        return path.substring(0, path.length() - ".java".length()).replace(File.separator, ".");
    }

    private static List<Path> javaFiles() throws IOException {
        try (Stream<Path> tree = Files.walk(SOURCES)) {
            return tree.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static String contentOf(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException cannotRead) {
            throw new IllegalStateException("No se pudo leer " + file, cannotRead);
        }
    }
}
