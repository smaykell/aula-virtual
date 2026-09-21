package io.github.smaykell.aulavirtual.course.enrollment;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "enrollments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseEntity {

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    private Enrollment(UUID courseId, UUID studentId, EnrollmentPolicy policy, Instant moment) {
        this.courseId = courseId;
        this.studentId = studentId;
        restart(policy, moment);
    }

    public static Enrollment request(UUID courseId, UUID studentId, EnrollmentPolicy policy,
            Instant moment) {

        return new Enrollment(courseId, studentId, policy, moment);
    }

    public final void restart(EnrollmentPolicy policy, Instant moment) {
        this.status = policy.initialStatus();
        this.requestedAt = moment;
        this.decidedAt = isPending() ? null : moment;
    }

    public void accept(Instant moment) {
        decide(EnrollmentStatus.ACTIVE, moment);
    }

    public void reject(Instant moment) {
        decide(EnrollmentStatus.REJECTED, moment);
    }

    public void withdraw(Instant moment) {
        decide(EnrollmentStatus.WITHDRAWN, moment);
    }

    public boolean isPending() {
        return status == EnrollmentStatus.PENDING;
    }

    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE;
    }

    private void decide(EnrollmentStatus decision, Instant moment) {
        this.status = decision;
        this.decidedAt = moment;
    }
}
