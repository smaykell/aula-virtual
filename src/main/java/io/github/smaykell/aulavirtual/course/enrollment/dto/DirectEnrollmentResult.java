package io.github.smaykell.aulavirtual.course.enrollment.dto;

public record DirectEnrollmentResult(
        String documentNumber,
        DirectEnrollmentOutcome outcome,
        EnrollmentResponse enrollment) {

    public static DirectEnrollmentResult skipped(String documentNumber,
            DirectEnrollmentOutcome outcome) {

        return new DirectEnrollmentResult(documentNumber, outcome, null);
    }
}
