package io.github.smaykell.aulavirtual.user.dto;

import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import java.util.List;

public record MeResponse(
        String username,
        List<RoleAccess> roles,
        PersonResponse person,
        boolean mustChangePassword) {
}
