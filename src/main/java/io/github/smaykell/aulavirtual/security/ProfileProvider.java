package io.github.smaykell.aulavirtual.security;

import java.util.Optional;
import java.util.UUID;

public interface ProfileProvider {

    Optional<Profile> activeProfileOf(UUID personId);
}
