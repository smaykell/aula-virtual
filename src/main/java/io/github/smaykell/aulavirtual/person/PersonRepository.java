package io.github.smaykell.aulavirtual.person;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findByDocumentTypeAndDocumentNumber(DocumentType documentType,
            String documentNumber);
}
