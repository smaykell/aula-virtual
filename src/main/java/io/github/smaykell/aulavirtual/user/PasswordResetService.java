package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.user.dto.PasswordResetCompletion;
import io.github.smaykell.aulavirtual.user.dto.PasswordResetRequest;
import io.github.smaykell.aulavirtual.user.exception.InvalidPasswordResetException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PersonService personService;
    private final NotificationService notificationService;
    private final PasswordResetTokens tokens;
    private final PasswordEncoder passwordEncoder;
    private final AccountProperties properties;
    private final LoginThrottle loginThrottle;
    private final Clock clock;

    // No responde nada, exista o no la cuenta: una ruta publica que dijera "no te
    // encuentro" serviria para averiguar quien tiene cuenta en el centro.
    @Transactional
    public void request(PasswordResetRequest request) {
        accountIdentifiedBy(request.identifier())
                .filter(this::outOfCooldown)
                .ifPresent(this::sendResetLink);
    }

    @Transactional
    public void complete(PasswordResetCompletion completion) {
        Instant now = clock.instant();
        PasswordReset reset = passwordResetRepository
                .findByTokenHash(tokens.hashOf(completion.token()))
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(InvalidPasswordResetException::new);

        User account = userRepository.findById(reset.getUserId())
                .orElseThrow(InvalidPasswordResetException::new);
        account.choosePassword(passwordEncoder.encode(completion.newPassword()));
        loginThrottle.forget(account.getUsername());
        passwordResetRepository.findByUserIdAndUsedAtIsNull(account.getId())
                .forEach(open -> open.use(now));
    }

    private Optional<User> accountIdentifiedBy(String identifier) {
        return userRepository.findByUsername(User.normalizeUsername(identifier))
                .or(() -> personService.findByEmail(identifier.trim().toLowerCase(Locale.ROOT))
                        .flatMap(person -> userRepository.findByPersonId(person.id())));
    }

    private boolean outOfCooldown(User account) {
        Instant since = clock.instant().minus(properties.passwordResetCooldown());
        return !passwordResetRepository.existsByUserIdAndRequestedAtAfter(account.getId(), since);
    }

    private void sendResetLink(User account) {
        PersonResponse person = personService.get(account.getPersonId());
        if (person.email() == null) {
            return;
        }
        String token = tokens.next();
        Instant now = clock.instant();
        passwordResetRepository.save(PasswordReset.issue(account.getId(), tokens.hashOf(token),
                now, now.plus(properties.passwordResetLifetime())));

        notificationService.enqueue(NotificationType.PASSWORD_RESET, person.email(), Map.of(
                "firstName", person.firstName(),
                "username", account.getUsername(),
                "link", properties.passwordResetUrl() + "?token=" + token,
                "minutes", String.valueOf(properties.passwordResetLifetime().toMinutes())));
    }
}
