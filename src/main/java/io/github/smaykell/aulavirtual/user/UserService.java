package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.PersonProfiles;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.dto.ChangeMyPasswordRequest;
import io.github.smaykell.aulavirtual.user.dto.Credentials;
import io.github.smaykell.aulavirtual.user.exception.AccountAlreadyExistsException;
import io.github.smaykell.aulavirtual.user.exception.AccountNotFoundException;
import io.github.smaykell.aulavirtual.user.exception.CredentialsRequiredException;
import io.github.smaykell.aulavirtual.user.exception.CurrentPasswordMismatchException;
import io.github.smaykell.aulavirtual.user.exception.InactiveActorException;
import io.github.smaykell.aulavirtual.user.exception.RoleOutOfReachException;
import io.github.smaykell.aulavirtual.user.exception.UnknownActorException;
import io.github.smaykell.aulavirtual.user.exception.UsernameTakenException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonProfiles personProfiles;

    @Transactional(readOnly = true)
    public Actor actor(String username) {
        User account = accountFor(username);
        Map<Role, UUID> profiles = personProfiles.of(account.getPersonId());
        if (profiles.isEmpty()) {
            throw new InactiveActorException();
        }
        return new Actor(account.getPersonId(), account.getUsername(), profiles);
    }

    @Transactional(readOnly = true)
    public Actor requireManagerOf(String actorUsername, Role role) {
        Actor actor = actor(actorUsername);
        if (!actor.canManage(role)) {
            throw new RoleOutOfReachException();
        }
        return actor;
    }

    @Transactional
    public void ensureAccount(UUID personId, Credentials credentials) {
        if (userRepository.existsByPersonId(personId)) {
            if (credentials != null) {
                throw new AccountAlreadyExistsException();
            }
            return;
        }
        if (credentials == null) {
            throw new CredentialsRequiredException();
        }
        String username = User.normalizeUsername(credentials.username());
        if (userRepository.existsByUsername(username)) {
            throw new UsernameTakenException();
        }
        userRepository.save(User.create(personId, username,
                passwordEncoder.encode(credentials.password())));
    }

    @Transactional
    public void changeOwnPassword(String username, ChangeMyPasswordRequest request) {
        User account = accountFor(username);
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new CurrentPasswordMismatchException();
        }
        account.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void changePasswordOf(UUID personId, String rawPassword) {
        accountOf(personId).changePassword(passwordEncoder.encode(rawPassword));
    }

    @Transactional(readOnly = true)
    public String usernameOf(UUID personId) {
        return accountOf(personId).getUsername();
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> usernamesByPersonId(Collection<UUID> personIds) {
        return userRepository.findByPersonIdIn(personIds).stream()
                .collect(Collectors.toMap(User::getPersonId, User::getUsername));
    }

    private User accountFor(String username) {
        return userRepository.findByUsername(User.normalizeUsername(username))
                .orElseThrow(UnknownActorException::new);
    }

    private User accountOf(UUID personId) {
        return userRepository.findByPersonId(personId)
                .orElseThrow(AccountNotFoundException::new);
    }
}
