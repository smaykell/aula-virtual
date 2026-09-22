package io.github.smaykell.aulavirtual.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> {

    Optional<PasswordReset> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndRequestedAtAfter(UUID userId, Instant moment);

    List<PasswordReset> findByUserIdAndUsedAtIsNull(UUID userId);
}
