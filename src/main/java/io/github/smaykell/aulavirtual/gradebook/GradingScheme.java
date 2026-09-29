package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grading_schemes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradingScheme extends BaseEntity {

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private GradingMethod method;

    @Column(name = "passing_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal passingScore;

    private GradingScheme(UUID courseId, GradingMethod method, BigDecimal passingScore) {
        this.courseId = courseId;
        update(method, passingScore);
    }

    public static GradingScheme byDefault(UUID courseId) {
        return new GradingScheme(courseId, GradingMethod.TOTAL_POINTS,
                GradingScale.DEFAULT_PASSING_SCORE);
    }

    public final void update(GradingMethod method, BigDecimal passingScore) {
        this.method = method;
        this.passingScore = passingScore;
    }
}
