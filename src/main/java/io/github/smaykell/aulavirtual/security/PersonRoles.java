package io.github.smaykell.aulavirtual.security;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersonRoles {

    private final List<RoleProvider> providers;

    public Set<Role> of(UUID personId) {
        return providers.stream()
                .map(provider -> provider.activeRoleOf(personId))
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
    }
}
