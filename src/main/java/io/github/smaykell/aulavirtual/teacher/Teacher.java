package io.github.smaykell.aulavirtual.teacher;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teachers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Teacher extends BaseEntity {

    @Column(name = "person_id", nullable = false, unique = true, updatable = false)
    private UUID personId;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Teacher(UUID personId) {
        this.personId = personId;
        this.active = true;
    }

    public static Teacher create(UUID personId) {
        return new Teacher(personId);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
