package io.github.smaykell.aulavirtual.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Person extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 10)
    private Sex sex;

    protected Person(String firstName, String lastName, LocalDate birthDate, Sex sex) {
        updatePersonalData(firstName, lastName, birthDate, sex);
    }

    public final void updatePersonalData(String firstName, String lastName, LocalDate birthDate,
            Sex sex) {

        this.firstName = collapseSpaces(firstName);
        this.lastName = collapseSpaces(lastName);
        this.birthDate = birthDate;
        this.sex = sex;
    }

    private static String collapseSpaces(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }
}
