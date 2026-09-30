package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.user.exception.TooManyLoginAttemptsException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginThrottle {

    private static final int PRUNE_THRESHOLD = 10_000;

    private final LoginThrottleProperties properties;
    private final Clock clock;
    private final Map<String, Failures> failuresByUsername = new ConcurrentHashMap<>();

    public void ensureAllowed(String username) {
        Failures failures = failuresByUsername.get(username);
        if (failures != null && failures.lockedAt(clock.instant())) {
            throw new TooManyLoginAttemptsException();
        }
    }

    public void recordFailure(String username) {
        Instant now = clock.instant();
        failuresByUsername.compute(username,
                (key, previous) -> previous == null || previous.expiredAt(now)
                        ? Failures.first(now, properties)
                        : previous.next(now, properties));
        pruneIfCrowded(now);
    }

    public void forget(String username) {
        failuresByUsername.remove(username);
    }

    private void pruneIfCrowded(Instant now) {
        if (failuresByUsername.size() > PRUNE_THRESHOLD) {
            failuresByUsername.values().removeIf(failures -> failures.expiredAt(now));
        }
    }

    private record Failures(int count, Instant windowEnd, Instant lockedUntil) {

        static Failures first(Instant now, LoginThrottleProperties properties) {
            return new Failures(0, now.plus(properties.window()), Instant.MIN)
                    .next(now, properties);
        }

        Failures next(Instant now, LoginThrottleProperties properties) {
            int total = count + 1;
            Instant lock = total >= properties.maxFailures()
                    ? now.plus(properties.lockout())
                    : lockedUntil;
            return new Failures(total, windowEnd, lock);
        }

        boolean lockedAt(Instant now) {
            return now.isBefore(lockedUntil);
        }

        boolean expiredAt(Instant now) {
            return !lockedAt(now) && !now.isBefore(windowEnd);
        }
    }
}
