package io.github.smaykell.aulavirtual.security;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersonProfiles {

    private final List<ProfileProvider> providers;

    public Map<Role, UUID> of(UUID personId) {
        return providers.stream()
                .map(provider -> provider.activeProfileOf(personId))
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableMap(Profile::role, Profile::id));
    }

    public Set<Role> rolesOf(UUID personId) {
        return of(personId).keySet();
    }
}
