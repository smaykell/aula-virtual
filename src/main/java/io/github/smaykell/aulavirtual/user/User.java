package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(name = "person_id", nullable = false, unique = true, updatable = false)
    private UUID personId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    private User(UUID personId, String username, String passwordHash) {
        this.personId = personId;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public static User create(UUID personId, String username, String passwordHash) {
        return new User(personId, normalizeUsername(username), passwordHash);
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
