package io.github.smaykell.aulavirtual.user.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.List;

public record MeResponse(
        String username,
        List<Role> roles,
        List<String> permissions,
        PersonResponse person) {
}
