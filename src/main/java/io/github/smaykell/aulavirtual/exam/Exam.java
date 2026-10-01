package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.exam.dto.ExamData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam extends BaseEntity {

    @Column(name = "unit_id", nullable = false, updatable = false)
    private UUID unitId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "instructions", length = 4000)
    private String instructions;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "shuffle_questions", nullable = false)
    private boolean shuffleQuestions;

    @Column(name = "shuffle_options", nullable = false)
    private boolean shuffleOptions;

    @Column(name = "shows_answers", nullable = false)
    private boolean showsAnswers;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore = BigDecimal.ZERO;

    private Exam(UUID unitId, UUID courseId, ExamData data) {
        this.unitId = unitId;
        this.courseId = courseId;
        update(data);
    }

    public static Exam create(UUID unitId, UUID courseId, ExamData data) {
        return new Exam(unitId, courseId, data);
    }

    public final void update(ExamData data) {
        this.title = data.title().trim();
        this.instructions = trimmed(data.instructions());
        this.opensAt = data.opensAt();
        this.closesAt = data.closesAt();
        this.timeLimitMinutes = data.timeLimitMinutes();
        this.maxAttempts = data.maxAttempts();
        this.shuffleQuestions = data.shuffleQuestions();
        this.shuffleOptions = data.shuffleOptions();
        this.showsAnswers = data.showsAnswers();
        this.categoryId = data.categoryId();
    }

    public void scoreOutOf(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
