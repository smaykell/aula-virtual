package io.github.smaykell.aulavirtual.exam;

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
@Table(name = "exam_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamAttempt extends BaseEntity {

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "number", nullable = false, updatable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "deadline", nullable = false, updatable = false)
    private Instant deadline;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "seed", nullable = false, updatable = false)
    private long seed;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    private ExamAttempt(Exam exam, UUID studentId, int number, Instant startedAt, long seed) {
        this.examId = exam.getId();
        this.studentId = studentId;
        this.number = number;
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = startedAt;
        this.deadline = exam.deadlineFor(startedAt);
        this.seed = seed;
    }

    public static ExamAttempt start(Exam exam, UUID studentId, int number, Instant startedAt,
            long seed) {
        return new ExamAttempt(exam, studentId, number, startedAt, seed);
    }

    public boolean isInProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }

    public boolean isGraded() {
        return status == AttemptStatus.GRADED;
    }

    public boolean isExpiredAt(Instant moment) {
        return isInProgress() && !moment.isBefore(deadline);
    }

    public boolean acceptsAnswersAt(Instant moment) {
        return isInProgress() && moment.isBefore(deadline);
    }

    public boolean belongsTo(UUID student) {
        return studentId.equals(student);
    }

    public void close(Instant moment, Grading grading) {
        this.submittedAt = moment.isBefore(deadline) ? moment : deadline;
        settle(grading);
    }

    public void settle(Grading grading) {
        this.score = grading.score();
        this.status = grading.pendingReview() ? AttemptStatus.PENDING_REVIEW
                : AttemptStatus.GRADED;
    }
}
