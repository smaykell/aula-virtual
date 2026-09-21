package io.github.smaykell.aulavirtual.administrator.dto;

import io.github.smaykell.aulavirtual.administrator.Administrator;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Role;
import java.time.Instant;
import java.util.UUID;

public record AdministratorResponse(
        UUID id,
        PersonResponse person,
        String username,
        Role role,
        boolean active,
        Instant createdAt) {

    public static AdministratorResponse from(Administrator administrator, PersonResponse person,
            String username) {

        return new AdministratorResponse(administrator.getId(), person, username,
                administrator.getRole(), administrator.isActive(), administrator.getCreatedAt());
    }
}
