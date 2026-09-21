package io.github.smaykell.aulavirtual.course;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.course.dto.UpdateCourseRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "invitation_code", nullable = false, unique = true, length = 12,
            updatable = false)
    private String invitationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_policy", nullable = false, length = 20)
    private EnrollmentPolicy enrollmentPolicy;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    private Course(CreateCourseRequest request, UUID teacherId, String invitationCode) {
        this.name = request.name().trim();
        this.description = trimmed(request.description());
        this.teacherId = teacherId;
        this.invitationCode = invitationCode;
        this.status = CourseStatus.ACTIVE;
        this.enrollmentPolicy = request.enrollmentPolicy();
        this.startDate = request.startDate();
        this.endDate = request.endDate();
    }

    public static Course create(CreateCourseRequest request, UUID teacherId,
            String invitationCode) {

        return new Course(request, teacherId, invitationCode);
    }

    public void update(UpdateCourseRequest request) {
        this.name = request.name().trim();
        this.description = trimmed(request.description());
        this.teacherId = request.teacherId();
        this.enrollmentPolicy = request.enrollmentPolicy();
        this.startDate = request.startDate();
        this.endDate = request.endDate();
    }

    public void archive() {
        this.status = CourseStatus.ARCHIVED;
    }

    public void activate() {
        this.status = CourseStatus.ACTIVE;
    }

    public boolean acceptsEnrollmentsWithoutApproval() {
        return enrollmentPolicy == EnrollmentPolicy.AUTOMATIC;
    }

    public boolean isArchived() {
        return status == CourseStatus.ARCHIVED;
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
