package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.PersonProfiles;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.dto.LoginRequest;
import io.github.smaykell.aulavirtual.user.dto.LoginResponse;
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

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User account = userRepository.findByUsername(User.normalizeUsername(request.username()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        Set<Role> roles = personProfiles.rolesOf(account.getPersonId());
        if (roles.isEmpty()) {
            throw new InactiveAccountException();
        }
        return sessionFor(account, roles);
    }

    private LoginResponse sessionFor(User account, Set<Role> roles) {
        return new LoginResponse(
                jwtService.issueToken(account.getUsername(), Role.grantedAuthoritiesOf(roles)),
                TOKEN_TYPE,
                jwtService.tokenLifetime().toSeconds(),
                account.getUsername(),
                Role.sorted(roles),
                Role.permissionAuthoritiesOf(roles));
    }
}
