package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "units")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Unit extends BaseEntity {

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "position", nullable = false)
    private int position;

    private Unit(UUID courseId, String title, int position) {
        this.courseId = courseId;
        this.title = title.trim();
        this.position = position;
    }

    public static Unit create(UUID courseId, String title, int position) {
        return new Unit(courseId, title, position);
    }

    public void rename(String title) {
        this.title = title.trim();
    }

    public void moveTo(int position) {
        this.position = position;
    }
}
