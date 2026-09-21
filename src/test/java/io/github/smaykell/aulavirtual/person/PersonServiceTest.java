package io.github.smaykell.aulavirtual.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 20);

    @Mock
    private PersonRepository personRepository;

    private PersonService personService;

    @BeforeEach
    void setUp() {
        personService = new PersonService(personRepository);
    }

    @Test
    void an_unknown_document_creates_the_person() {
        UUID newId = UUID.randomUUID();
        givenNobodyHasTheDocument("45678912");
        givenTheStoredPersonGetsTheId(newId);

        UUID personId = personService.resolveOrCreate(dataOf("45678912", "Juan Carlos"));

        assertThat(personId).isEqualTo(newId);
    }

    @Test
    void a_known_document_reuses_the_person_instead_of_creating_another_one() {
        UUID existingId = UUID.randomUUID();
        givenTheDocumentBelongsTo("45678912", existingId, "Juan Carlos");

        UUID personId = personService.resolveOrCreate(dataOf("45678912", "Otro Nombre"));

        assertThat(personId).isEqualTo(existingId);
        verify(personRepository, never()).save(any(Person.class));
    }

    @Test
    void reusing_a_person_does_not_overwrite_the_data_already_stored() {
        UUID existingId = UUID.randomUUID();
        Person stored = givenTheDocumentBelongsTo("45678912", existingId, "Juan Carlos");

        personService.resolveOrCreate(dataOf("45678912", "Nombre Nuevo"));

        assertThat(stored.getFirstName()).isEqualTo("Juan Carlos");
    }

    @Test
    void a_document_that_does_not_match_its_type_is_rejected() {
        givenNobodyHasTheDocument("4567");

        ApiException error = assertThrows(ApiException.class,
                () -> personService.resolveOrCreate(dataOf("4567", "Juan Carlos")));

        assertThat(error.getCode()).isEqualTo("PRS_INVALID_DOCUMENT_NUMBER");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updating_a_person_with_the_document_of_another_one_is_rejected() {
        UUID personId = UUID.randomUUID();
        Person target = Person.create(dataOf("87654321", "Juan Carlos"));
        ReflectionTestUtils.setField(target, "id", personId);
        when(personRepository.findById(personId)).thenReturn(Optional.of(target));
        givenTheDocumentBelongsTo("45678912", UUID.randomUUID(), "Otra Persona");

        ApiException error = assertThrows(ApiException.class,
                () -> personService.update(personId, dataOf("45678912", "Juan Carlos")));

        assertThat(error.getCode()).isEqualTo("PRS_DOCUMENT_TAKEN");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updating_a_person_keeping_its_own_document_changes_the_data() {
        UUID personId = UUID.randomUUID();
        Person stored = givenTheDocumentBelongsTo("45678912", personId, "Juan Carlos");
        when(personRepository.findById(personId)).thenReturn(Optional.of(stored));

        PersonResponse updated = personService.update(personId,
                dataOf("45678912", "Juan Carlos Alberto"));

        assertThat(updated.firstName()).isEqualTo("Juan Carlos Alberto");
    }

    @Test
    void an_unknown_person_cannot_be_updated() {
        UUID personId = UUID.randomUUID();
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> personService.update(personId, dataOf("45678912", "Juan Carlos")));

        assertThat(error.getCode()).isEqualTo("PRS_NOT_FOUND");
    }

    @Test
    void the_names_lose_their_extra_spaces() {
        givenNobodyHasTheDocument("45678912");
        givenTheStoredPersonGetsTheId(UUID.randomUUID());

        personService.resolveOrCreate(new PersonData(DocumentType.DNI, " 45678912 ",
                "  Juan   Carlos ", " Perez  Gomez ", BIRTH_DATE, Sex.MALE));

        verify(personRepository).save(argThat(person ->
                person.getFirstName().equals("Juan Carlos")
                        && person.getLastName().equals("Perez Gomez")
                        && person.getDocumentNumber().equals("45678912")));
    }

    private void givenTheStoredPersonGetsTheId(UUID personId) {
        when(personRepository.save(any(Person.class))).thenAnswer(call -> {
            Person person = call.getArgument(0);
            ReflectionTestUtils.setField(person, "id", personId);
            return person;
        });
    }

    private void givenNobodyHasTheDocument(String documentNumber) {
        when(personRepository.findByDocumentTypeAndDocumentNumber(DocumentType.DNI, documentNumber))
                .thenReturn(Optional.empty());
    }

    private Person givenTheDocumentBelongsTo(String documentNumber, UUID personId,
            String firstName) {

        Person person = Person.create(dataOf(documentNumber, firstName));
        ReflectionTestUtils.setField(person, "id", personId);
        when(personRepository.findByDocumentTypeAndDocumentNumber(DocumentType.DNI, documentNumber))
                .thenReturn(Optional.of(person));
        return person;
    }

    private static PersonData dataOf(String documentNumber, String firstName) {
        return new PersonData(DocumentType.DNI, documentNumber, firstName, "Perez Gomez",
                BIRTH_DATE, Sex.MALE);
    }
}
