package io.github.smaykell.aulavirtual.modules.administrator;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "administrators")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Administrator extends BaseEntity {

    @Column(name = "person_id", nullable = false, unique = true, updatable = false)
    private UUID personId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20, updatable = false)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Administrator(UUID personId, Role role) {
        this.personId = personId;
        this.role = role;
        this.active = true;
    }

    public static Administrator create(UUID personId) {
        return new Administrator(personId, Role.ADMIN);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
