package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.PersonProfiles;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.dto.LoginRequest;
import io.github.smaykell.aulavirtual.user.dto.LoginResponse;
import io.github.smaykell.aulavirtual.user.dto.RoleAccess;
import io.github.smaykell.aulavirtual.user.exception.InactiveAccountException;
import io.github.smaykell.aulavirtual.user.exception.InvalidCredentialsException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonProfiles personProfiles;
    private final JwtService jwtService;
    private final LoginThrottle loginThrottle;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = User.normalizeUsername(request.username());
        loginThrottle.ensureAllowed(username);
        User account = userRepository.findByUsername(username)
                .filter(candidate -> passwordEncoder.matches(request.password(),
                        candidate.getPasswordHash()))
                .orElseThrow(() -> rejected(username));
        loginThrottle.forget(username);

        Set<Role> roles = personProfiles.rolesOf(account.getPersonId());
        if (roles.isEmpty()) {
            throw new InactiveAccountException();
        }
        return sessionFor(account, roles);
    }

    private InvalidCredentialsException rejected(String username) {
        loginThrottle.recordFailure(username);
        return new InvalidCredentialsException();
    }

    private LoginResponse sessionFor(User account, Set<Role> roles) {
        return new LoginResponse(
                jwtService.issueToken(account.getUsername(), Role.grantedAuthoritiesOf(roles)),
                TOKEN_TYPE,
                jwtService.tokenLifetime().toSeconds(),
                account.getUsername(),
                RoleAccess.of(roles),
                account.isMustChangePassword());
    }
}
