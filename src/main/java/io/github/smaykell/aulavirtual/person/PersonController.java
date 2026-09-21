package io.github.smaykell.aulavirtual.person;

import io.github.smaykell.aulavirtual.person.dto.PersonLookupResponse;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.person.exception.DocumentNotFoundException;
import io.github.smaykell.aulavirtual.security.Permission;
import io.github.smaykell.aulavirtual.security.PersonProfiles;
import io.github.smaykell.aulavirtual.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final PersonProfiles personProfiles;

    @GetMapping("/$byDocument")
    @PreAuthorize("hasAnyAuthority('" + Permission.Name.TEACHERS_CREATE + "', '"
            + Permission.Name.ADMINISTRATORS_CREATE + "')")
    public PersonLookupResponse byDocument(@RequestParam DocumentType documentType,
            @RequestParam String documentNumber) {

        PersonResponse person = personService.findByDocument(documentType, documentNumber)
                .orElseThrow(DocumentNotFoundException::new);
        return new PersonLookupResponse(person, Role.sorted(personProfiles.rolesOf(person.id())));
    }
}
