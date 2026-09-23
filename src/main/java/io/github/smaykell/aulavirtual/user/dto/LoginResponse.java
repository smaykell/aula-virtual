package io.github.smaykell.aulavirtual.user.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        List<RoleAccess> roles) {
}
