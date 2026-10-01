package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exam_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamQuestion extends BaseEntity {

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "position", nullable = false, updatable = false)
    private int position;

    @Column(name = "points", nullable = false, precision = 5, scale = 2, updatable = false)
    private BigDecimal points;

    private ExamQuestion(UUID examId, UUID questionId, int position, BigDecimal points) {
        this.examId = examId;
        this.questionId = questionId;
        this.position = position;
        this.points = points;
    }

    public static ExamQuestion of(UUID examId, UUID questionId, int position,
            BigDecimal points) {
        return new ExamQuestion(examId, questionId, position, points);
    }
}
