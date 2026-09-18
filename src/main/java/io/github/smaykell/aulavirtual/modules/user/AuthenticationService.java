package io.github.smaykell.aulavirtual.modules.user;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.user.dto.LoginRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.LoginResponse;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.Permission;
import io.github.smaykell.aulavirtual.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";
    private static final String INACTIVE_ACCOUNT =
            "Tu cuenta está desactivada. Contacta al administrador.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(User.normalizeUsername(request.username()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, INACTIVE_ACCOUNT);
        }
        return sessionFor(user);
    }

    private LoginResponse sessionFor(User user) {
        Role role = user.getRole();
        return new LoginResponse(
                jwtService.issueToken(user.getUsername(), role.grantedAuthorities()),
                TOKEN_TYPE,
                jwtService.tokenLifetime().toSeconds(),
                user.getUsername(),
                role,
                role.permissions().stream().map(Permission::authority).toList());
    }
}
