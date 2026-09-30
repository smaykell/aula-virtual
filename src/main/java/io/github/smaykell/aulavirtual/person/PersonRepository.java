package io.github.smaykell.aulavirtual.person;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findByDocumentTypeAndDocumentNumber(DocumentType documentType,
            String documentNumber);

    Optional<Person> findByEmail(String email);

    @Query(value = """
            select id from persons
            where unaccent(lower(first_name || ' ' || last_name)) like unaccent(:pattern)
               or unaccent(lower(last_name || ' ' || first_name)) like unaccent(:pattern)
               or lower(document_number) like :pattern
               or lower(email) like :pattern
            """, nativeQuery = true)
    List<UUID> findIdsMatching(@Param("pattern") String pattern);
}
