package io.github.smaykell.aulavirtual.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.Sex;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.settings.SettingsService;
import io.github.smaykell.aulavirtual.settings.StudentIdentifier;
import io.github.smaykell.aulavirtual.settings.dto.SettingsResponse;
import io.github.smaykell.aulavirtual.user.dto.Credentials;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentCredentialsTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 20);

    @Mock
    private SettingsService settingsService;

    private StudentCredentials studentCredentials;

    @BeforeEach
    void setUp() {
        studentCredentials = new StudentCredentials(settingsService);
    }

    @Test
    void the_document_is_the_username_and_also_the_first_password() {
        givenTheCentreIdentifiesBy(StudentIdentifier.DOCUMENT_NUMBER);

        Credentials credentials = studentCredentials.forNewAccount(personWith(" 45678912 ", null));

        assertThat(credentials.username()).isEqualTo("45678912");
        assertThat(credentials.password()).isEqualTo("45678912");
    }

    @Test
    void identifying_by_email_still_leaves_the_document_as_the_first_password() {
        givenTheCentreIdentifiesBy(StudentIdentifier.EMAIL);

        Credentials credentials = studentCredentials
                .forNewAccount(personWith("45678912", " Ana@Escuela.PE "));

        assertThat(credentials.username()).isEqualTo("ana@escuela.pe");
        assertThat(credentials.password()).isEqualTo("45678912");
    }

    @Test
    void identifying_by_email_without_an_email_says_so_instead_of_building_a_broken_account() {
        givenTheCentreIdentifiesBy(StudentIdentifier.EMAIL);

        ApiException error = assertThrows(ApiException.class,
                () -> studentCredentials.forNewAccount(personWith("45678912", null)));

        assertThat(error.getCode()).isEqualTo("PRS_EMAIL_REQUIRED");
    }

    @Test
    void a_centre_that_assigns_the_usernames_by_hand_does_not_derive_any() {
        givenTheCentreIdentifiesBy(StudentIdentifier.MANUAL);

        ApiException error = assertThrows(ApiException.class,
                () -> studentCredentials.forNewAccount(personWith("45678912", "ana@escuela.pe")));

        assertThat(error.getCode()).isEqualTo("SET_MANUAL_IDENTIFIER");
    }

    private void givenTheCentreIdentifiesBy(StudentIdentifier identifier) {
        when(settingsService.current()).thenReturn(new SettingsResponse(identifier, true));
    }

    private static PersonData personWith(String documentNumber, String email) {
        return new PersonData(DocumentType.DNI, documentNumber, "Ana Maria", "Quispe Rojas",
                BIRTH_DATE, Sex.FEMALE, email);
    }
}
