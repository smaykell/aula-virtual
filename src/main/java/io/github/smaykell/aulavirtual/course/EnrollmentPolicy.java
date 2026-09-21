package io.github.smaykell.aulavirtual.course;

public enum EnrollmentPolicy {

    AUTOMATIC,
    ON_REQUEST;

    public EnrollmentStatus initialStatus() {
        return this == AUTOMATIC ? EnrollmentStatus.ACTIVE : EnrollmentStatus.PENDING;
    }
}
