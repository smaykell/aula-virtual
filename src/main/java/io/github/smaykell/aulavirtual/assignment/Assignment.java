package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.AssignmentData;
import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
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
@Table(name = "assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity {

    @Column(name = "unit_id", nullable = false, updatable = false)
    private UUID unitId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "instructions", length = 4000)
    private String instructions;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "allows_late", nullable = false)
    private boolean allowsLate;

    @Column(name = "attachment_key", length = 255)
    private String attachmentKey;

    private Assignment(UUID unitId, AssignmentData data) {
        this.unitId = unitId;
        update(data);
    }

    public static Assignment create(UUID unitId, AssignmentData data) {
        return new Assignment(unitId, data);
    }

    public final void update(AssignmentData data) {
        this.title = data.title().trim();
        this.instructions = trimmed(data.instructions());
        this.dueAt = data.dueAt();
        this.maxScore = data.maxScore();
        this.allowsLate = data.allowsLate();
        this.attachmentKey = trimmed(data.attachmentKey());
    }

    public boolean isLate(Instant moment) {
        return moment.isAfter(dueAt);
    }

    public boolean acceptsAt(Instant moment) {
        return allowsLate || !isLate(moment);
    }

    public boolean accepts(BigDecimal score) {
        return score.signum() >= 0 && score.compareTo(maxScore) <= 0;
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
