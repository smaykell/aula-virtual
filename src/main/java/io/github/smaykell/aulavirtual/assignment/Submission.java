package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.SubmissionData;
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
@Table(name = "submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends BaseEntity {

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private UUID assignmentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "text", length = 10000)
    private String text;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    private Submission(UUID assignmentId, UUID studentId, SubmissionData data, Instant moment,
            SubmissionStatus status) {

        this.assignmentId = assignmentId;
        this.studentId = studentId;
        replace(data, moment, status);
    }

    public static Submission of(UUID assignmentId, UUID studentId, SubmissionData data,
            Instant moment, SubmissionStatus status) {

        return new Submission(assignmentId, studentId, data, moment, status);
    }

    public final void replace(SubmissionData data, Instant moment, SubmissionStatus status) {
        this.storageKey = trimmed(data.storageKey());
        this.text = trimmed(data.text());
        this.submittedAt = moment;
        this.status = status;
    }

    public void markGraded() {
        this.status = SubmissionStatus.GRADED;
    }

    public boolean isGraded() {
        return status == SubmissionStatus.GRADED;
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
