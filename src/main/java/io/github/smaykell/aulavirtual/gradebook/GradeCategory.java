package io.github.smaykell.aulavirtual.gradebook;

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
@Table(name = "grade_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeCategory extends BaseEntity {

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "position", nullable = false)
    private int position;

    private GradeCategory(UUID courseId, String name, BigDecimal weight, int position) {
        this.courseId = courseId;
        update(name, weight, position);
    }

    public static GradeCategory create(UUID courseId, String name, BigDecimal weight,
            int position) {

        return new GradeCategory(courseId, name, weight, position);
    }

    public final void update(String name, BigDecimal weight, int position) {
        this.name = name.trim();
        this.weight = weight;
        this.position = position;
    }
}
