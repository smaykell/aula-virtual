package io.github.smaykell.aulavirtual.modules.user.dto;

import io.github.smaykell.aulavirtual.modules.user.User;
import io.github.smaykell.aulavirtual.security.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        Role role,
        boolean active,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isActive(),
                user.getCreatedAt());
    }
}
