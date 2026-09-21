package io.github.smaykell.aulavirtual.course;

import io.github.smaykell.aulavirtual.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.course.dto.InvitationResponse;
import io.github.smaykell.aulavirtual.course.dto.MaterialData;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherSummary;
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
        return course(teacherId, EnrollmentPolicy.ON_REQUEST);
    }

    static Course course(UUID teacherId, EnrollmentPolicy policy) {
        Course course = Course.create(createRequest(teacherId, policy), teacherId,
                INVITATION_CODE);
        return withId(course, UUID.randomUUID());
    }

    static CreateCourseRequest createRequest(UUID teacherId) {
        return createRequest(teacherId, EnrollmentPolicy.ON_REQUEST);
    }

    static CreateCourseRequest createRequest(UUID teacherId, EnrollmentPolicy policy) {
        return new CreateCourseRequest("Algebra Lineal", "Curso del primer ciclo", teacherId,
                policy, START, END);
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

    static MaterialData hidden(String title, MaterialType type) {
        return new MaterialData(title, type, "courses/algebra/borrador.pdf", null, null, false);
    }

    static MaterialData link(String title) {
        return new MaterialData(title, MaterialType.LINK, null, "https://example.org/clase",
                null, true);
    }

    static InvitationResponse invitation() {
        return new InvitationResponse(INVITATION_CODE, INVITATION_URL);
    }

    static Enrollment enrollment(UUID courseId, UUID studentId, EnrollmentPolicy policy,
            Instant moment) {

        return withId(Enrollment.request(courseId, studentId, policy, moment),
                UUID.randomUUID());
    }

    static StudentSummary student(UUID studentId) {
        return new StudentSummary(studentId, "Ana Maria", "Quispe Rojas", true);
    }

    static TeacherSummary teacher(UUID teacherId) {
        return new TeacherSummary(teacherId, "Juan Carlos", "Perez Gomez", true);
    }

    private static <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
