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

    @Column(name = "workplace", length = 160)
    private String workplace;

    private Student(UUID personId, String workplace) {
        this.personId = personId;
        this.workplace = normalizeWorkplace(workplace);
        this.active = true;
    }

    public static Student create(UUID personId, String workplace) {
        return new Student(personId, workplace);
    }

    public void update(String workplace) {
        this.workplace = normalizeWorkplace(workplace);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String normalizeWorkplace(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\s+", " ");
    }
}
