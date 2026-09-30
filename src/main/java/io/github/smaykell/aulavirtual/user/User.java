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

    @Column(name = "username", nullable = false, unique = true, length = 160)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    private User(UUID personId, String username, String passwordHash) {
        this.personId = personId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.mustChangePassword = true;
    }

    public static User create(UUID personId, String username, String passwordHash) {
        return new User(personId, normalizeUsername(username), passwordHash);
    }

    public void assignPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = true;
    }

    public void choosePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = false;
    }

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
