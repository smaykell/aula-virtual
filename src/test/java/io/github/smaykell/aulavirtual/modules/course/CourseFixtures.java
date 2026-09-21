package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.modules.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.modules.course.dto.InvitationResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.MaterialData;
import io.github.smaykell.aulavirtual.modules.teacher.dto.TeacherSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

final class CourseFixtures {

    static final LocalDate START = LocalDate.of(2026, 3, 1);
    static final LocalDate END = LocalDate.of(2026, 7, 15);
    static final String INVITATION_CODE = "ABCD2345";
    static final String INVITATION_URL = "https://aula.example/join/ABCD2345";

    private CourseFixtures() {
    }

    static Course course(UUID teacherId) {
        Course course = Course.create(createRequest(teacherId), teacherId, INVITATION_CODE);
        return withId(course, UUID.randomUUID());
    }

    static CreateCourseRequest createRequest(UUID teacherId) {
        return new CreateCourseRequest("Algebra Lineal", "Curso del primer ciclo", teacherId,
                START, END);
    }

    static Unit unit(UUID courseId, String title, int position) {
        return withId(Unit.create(courseId, title, position), UUID.randomUUID());
    }

    static Material material(UUID unitId, MaterialData data, Instant publishedAt) {
        return withId(Material.create(unitId, data, publishedAt), UUID.randomUUID());
    }

    static MaterialData file(String title, MaterialType type) {
        return new MaterialData(title, type, "courses/algebra/tema-1.pdf", null, null, true);
    }

    static MaterialData link(String title) {
        return new MaterialData(title, MaterialType.LINK, null, "https://example.org/clase",
                null, true);
    }

    static InvitationResponse invitation() {
        return new InvitationResponse(INVITATION_CODE, INVITATION_URL);
    }

    static TeacherSummary teacher(UUID teacherId) {
        return new TeacherSummary(teacherId, "Juan Carlos", "Perez Gomez", true);
    }

    private static <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
