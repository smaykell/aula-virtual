package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.AssignmentData;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionData;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

final class AssignmentFixtures {

    static final Instant NOW = Instant.parse("2026-04-10T09:00:00Z");
    static final Instant DUE_AT = Instant.parse("2026-04-20T23:59:00Z");
    static final BigDecimal MAX_SCORE = new BigDecimal("20.00");

    private AssignmentFixtures() {
    }

    static Assignment assignment(UUID unitId) {
        return assignment(unitId, DUE_AT, false);
    }

    static Assignment assignment(UUID unitId, Instant dueAt, boolean allowsLate) {
        return withId(Assignment.create(unitId, data(dueAt, allowsLate)), UUID.randomUUID());
    }

    static AssignmentData data() {
        return data(DUE_AT, false);
    }

    static AssignmentData data(Instant dueAt, boolean allowsLate) {
        return new AssignmentData("Practica 1", "Resuelve los ejercicios del capitulo 2",
                dueAt, MAX_SCORE, allowsLate, null);
    }

    static Submission submission(UUID assignmentId, UUID studentId, Instant moment,
            SubmissionStatus status) {

        return withId(Submission.of(assignmentId, studentId, text("Mi respuesta"), moment,
                status), UUID.randomUUID());
    }

    static SubmissionData text(String text) {
        return new SubmissionData(null, text);
    }

    static StudentSummary student(UUID studentId) {
        return new StudentSummary(studentId, "Ana Maria", "Quispe Rojas", "Hospital Regional", true);
    }

    private static <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
