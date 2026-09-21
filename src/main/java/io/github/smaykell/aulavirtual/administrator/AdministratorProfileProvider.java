package io.github.smaykell.aulavirtual.administrator;

import io.github.smaykell.aulavirtual.security.Profile;
import io.github.smaykell.aulavirtual.security.ProfileProvider;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdministratorProfileProvider implements ProfileProvider {

    private final AdministratorRepository administratorRepository;

    @Override
    public Optional<Profile> activeProfileOf(UUID personId) {
        return administratorRepository.findByPersonId(personId)
                .filter(Administrator::isActive)
                .map(administrator -> new Profile(administrator.getRole(),
                        administrator.getId()));
    }
}
