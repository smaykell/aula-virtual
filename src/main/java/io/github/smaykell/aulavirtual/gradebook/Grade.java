package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20, updatable = false)
    private GradeSource sourceType;

    @Column(name = "source_id", nullable = false, updatable = false)
    private UUID sourceId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "feedback", length = 4000)
    private String feedback;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "graded_at", nullable = false)
    private Instant gradedAt;

    private Grade(GradeSource sourceType, UUID sourceId, UUID studentId, UUID courseId) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.studentId = studentId;
        this.courseId = courseId;
    }

    public static Grade of(GradeSource sourceType, UUID sourceId, UUID studentId, UUID courseId) {
        return new Grade(sourceType, sourceId, studentId, courseId);
    }

    public void record(BigDecimal score, BigDecimal maxScore, String feedback, UUID gradedBy,
            Instant moment) {

        this.score = score;
        this.maxScore = maxScore;
        this.feedback = feedback == null || feedback.isBlank() ? null : feedback.trim();
        this.gradedBy = gradedBy;
        this.gradedAt = moment;
    }
}
