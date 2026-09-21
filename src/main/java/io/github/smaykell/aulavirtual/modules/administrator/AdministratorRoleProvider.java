package io.github.smaykell.aulavirtual.modules.administrator;

import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.RoleProvider;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdministratorRoleProvider implements RoleProvider {

    private final AdministratorRepository administratorRepository;

    @Override
    public Optional<Role> activeRoleOf(UUID personId) {
        return administratorRepository.findByPersonId(personId)
                .filter(Administrator::isActive)
                .map(Administrator::getRole);
    }
}
