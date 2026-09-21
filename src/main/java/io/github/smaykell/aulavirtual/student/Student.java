package io.github.smaykell.aulavirtual.student;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseEntity {

    @Column(name = "person_id", nullable = false, unique = true, updatable = false)
    private UUID personId;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Student(UUID personId) {
        this.personId = personId;
        this.active = true;
    }

    public static Student create(UUID personId) {
        return new Student(personId);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
