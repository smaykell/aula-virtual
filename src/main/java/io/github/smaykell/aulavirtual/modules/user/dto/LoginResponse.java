package io.github.smaykell.aulavirtual.modules.user.dto;

import io.github.smaykell.aulavirtual.security.Role;
import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        List<Role> roles,
        List<String> permissions) {
}
