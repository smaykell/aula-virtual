package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attempt_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttemptAnswer extends BaseEntity {

    @Column(name = "attempt_id", nullable = false, updatable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "selected_option_ids")
    private List<UUID> selectedOptionIds;

    @Column(name = "text", length = 4000)
    private String text;

    @Column(name = "points", precision = 5, scale = 2)
    private BigDecimal points;

    @Column(name = "feedback", length = 2000)
    private String feedback;

    private AttemptAnswer(UUID attemptId, UUID questionId) {
        this.attemptId = attemptId;
        this.questionId = questionId;
    }

    public static AttemptAnswer of(UUID attemptId, UUID questionId) {
        return new AttemptAnswer(attemptId, questionId);
    }

    public void replace(List<UUID> selectedOptionIds, String text) {
        this.selectedOptionIds = selectedOptionIds;
        this.text = text;
    }

    public List<UUID> selectedOrNone() {
        return selectedOptionIds == null ? List.of() : selectedOptionIds;
    }

    public boolean isBlank() {
        return selectedOrNone().isEmpty() && (text == null || text.isBlank());
    }

    public boolean isScored() {
        return points != null;
    }

    public void award(BigDecimal points, String feedback) {
        this.points = points;
        this.feedback = feedback == null || feedback.isBlank() ? null : feedback.trim();
    }
}
