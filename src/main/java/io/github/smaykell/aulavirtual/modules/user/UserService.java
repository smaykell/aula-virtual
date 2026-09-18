package io.github.smaykell.aulavirtual.modules.user;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.user.dto.CreateUserRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.UserResponse;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String UNKNOWN_ACTOR = "Tu sesión ya no es válida. Vuelve a iniciar sesión.";
    private static final String INACTIVE_ACTOR =
            "Tu cuenta está desactivada. Contacta al administrador.";
    private static final String ROLE_OUT_OF_REACH =
            "No tienes permisos para administrar usuarios con ese rol";
    private static final String USERNAME_TAKEN = "Ya existe un usuario con ese nombre";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String actorUsername, Boolean active, Pageable pageable) {
        Set<Role> reachableRoles = activeActor(actorUsername).getRole().manageableRoles();
        Page<User> users = active == null
                ? userRepository.findByRoleIn(reachableRoles, pageable)
                : userRepository.findByRoleInAndActive(reachableRoles, active, pageable);
        return PageResponse.of(users, UserResponse::from);
    }

    @Transactional
    public UserResponse create(String actorUsername, CreateUserRequest request) {
        User actor = activeActor(actorUsername);
        if (!actor.getRole().canManage(request.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ROLE_OUT_OF_REACH);
        }

        String username = User.normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict(USERNAME_TAKEN);
        }

        User created = userRepository.save(
                User.create(username, passwordEncoder.encode(request.password()), request.role()));
        return UserResponse.from(created);
    }

    private User activeActor(String username) {
        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, UNKNOWN_ACTOR));
        if (!actor.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, INACTIVE_ACTOR);
        }
        return actor;
    }
}
