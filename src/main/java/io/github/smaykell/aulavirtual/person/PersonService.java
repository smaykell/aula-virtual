package io.github.smaykell.aulavirtual.person;

import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.person.exception.DocumentTakenException;
import io.github.smaykell.aulavirtual.person.exception.EmailTakenException;
import io.github.smaykell.aulavirtual.person.exception.InvalidDocumentNumberException;
import io.github.smaykell.aulavirtual.person.exception.PersonNotFoundException;
import io.github.smaykell.aulavirtual.common.domain.Filters;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    @Transactional
    public UUID resolveOrCreate(PersonData data) {
        return findEntityByDocument(data)
                .map(Person::getId)
                .orElseGet(() -> create(data).getId());
    }

    @Transactional
    public PersonResponse update(UUID personId, PersonData data) {
        Person person = existing(personId);
        requireFreeDocument(data, personId);
        requireFreeEmail(data, personId);
        requireValidDocument(data);
        person.update(data);
        return PersonResponse.from(person);
    }

    @Transactional(readOnly = true)
    public PersonResponse get(UUID personId) {
        return PersonResponse.from(existing(personId));
    }

    @Transactional(readOnly = true)
    public Optional<PersonResponse> findByDocument(DocumentType documentType,
            String documentNumber) {

        return personRepository
                .findByDocumentTypeAndDocumentNumber(documentType, documentNumber.trim())
                .map(PersonResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<PersonResponse> findByEmail(String email) {
        return personRepository.findByEmail(email).map(PersonResponse::from);
    }

    @Transactional(readOnly = true)
    public Map<UUID, PersonResponse> byIds(Collection<UUID> personIds) {
        return personRepository.findAllById(personIds).stream()
                .collect(Collectors.toMap(Person::getId, PersonResponse::from));
    }

    @Transactional(readOnly = true)
    public List<UUID> idsMatching(String search) {
        return personRepository.findIdsMatching(Filters.containing(search));
    }

    private Person create(PersonData data) {
        requireValidDocument(data);
        requireFreeEmail(data, null);
        return personRepository.save(Person.create(data));
    }

    private void requireFreeDocument(PersonData data, UUID owner) {
        findEntityByDocument(data)
                .filter(other -> !other.getId().equals(owner))
                .ifPresent(other -> {
                    throw new DocumentTakenException();
                });
    }

    private void requireFreeEmail(PersonData data, UUID owner) {
        String email = data.normalizedEmail();
        if (email == null) {
            return;
        }
        personRepository.findByEmail(email)
                .filter(other -> !other.getId().equals(owner))
                .ifPresent(other -> {
                    throw new EmailTakenException();
                });
    }

    private Optional<Person> findEntityByDocument(PersonData data) {
        return personRepository.findByDocumentTypeAndDocumentNumber(
                data.documentType(), data.documentNumber().trim());
    }

    private Person existing(UUID personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
    }

    private static void requireValidDocument(PersonData data) {
        if (!data.documentType().accepts(data.documentNumber().trim())) {
            throw new InvalidDocumentNumberException();
        }
    }
}
