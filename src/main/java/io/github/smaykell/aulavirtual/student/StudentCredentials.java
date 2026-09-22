package io.github.smaykell.aulavirtual.student;

import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.exception.EmailRequiredException;
import io.github.smaykell.aulavirtual.settings.SettingsService;
import io.github.smaykell.aulavirtual.settings.exception.ManualIdentifierException;
import io.github.smaykell.aulavirtual.user.dto.Credentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StudentCredentials {

    private final SettingsService settingsService;

    Credentials forNewAccount(PersonData person) {
        String document = person.documentNumber().trim();
        return new Credentials(usernameFor(person, document), document);
    }

    private String usernameFor(PersonData person, String document) {
        return switch (settingsService.current().studentIdentifier()) {
            case DOCUMENT_NUMBER -> document;
            case EMAIL -> requiredEmail(person);
            case MANUAL -> throw new ManualIdentifierException();
        };
    }

    private static String requiredEmail(PersonData person) {
        String email = person.normalizedEmail();
        if (email == null) {
            throw new EmailRequiredException();
        }
        return email;
    }
}
