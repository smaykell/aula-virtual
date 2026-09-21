package io.github.smaykell.aulavirtual.person;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "persons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 10)
    private Sex sex;

    private Person(PersonData data) {
        update(data);
    }

    public static Person create(PersonData data) {
        return new Person(data);
    }

    public final void update(PersonData data) {
        this.documentType = data.documentType();
        this.documentNumber = data.documentNumber().trim();
        this.firstName = collapseSpaces(data.firstName());
        this.lastName = collapseSpaces(data.lastName());
        this.birthDate = data.birthDate();
        this.sex = data.sex();
    }

    public String fullName() {
        return lastName + ", " + firstName;
    }

    private static String collapseSpaces(String value) {
        return value.trim().replaceAll("\s+", " ");
    }
}
