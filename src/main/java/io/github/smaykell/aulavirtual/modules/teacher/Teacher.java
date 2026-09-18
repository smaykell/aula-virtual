package io.github.smaykell.aulavirtual.modules.teacher;

import io.github.smaykell.aulavirtual.common.domain.Person;
import io.github.smaykell.aulavirtual.common.domain.Sex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teachers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Teacher extends Person {

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Teacher(UUID userId, String firstName, String lastName, LocalDate birthDate, Sex sex) {
        super(firstName, lastName, birthDate, sex);
        this.userId = userId;
        this.active = true;
    }

    public static Teacher create(UUID userId, String firstName, String lastName,
            LocalDate birthDate, Sex sex) {

        return new Teacher(userId, firstName, lastName, birthDate, sex);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
