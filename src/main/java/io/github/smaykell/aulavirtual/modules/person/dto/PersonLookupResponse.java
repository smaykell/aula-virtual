package io.github.smaykell.aulavirtual.modules.person.dto;

import io.github.smaykell.aulavirtual.security.Role;
import java.util.List;

public record PersonLookupResponse(
        PersonResponse person,
        List<Role> roles) {
}
