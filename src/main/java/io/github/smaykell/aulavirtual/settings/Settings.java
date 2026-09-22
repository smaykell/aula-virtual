package io.github.smaykell.aulavirtual.settings;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.settings.dto.UpdateSettingsRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settings extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "student_identifier", nullable = false, length = 20)
    private StudentIdentifier studentIdentifier;

    @Column(name = "self_registration_enabled", nullable = false)
    private boolean selfRegistrationEnabled;

    public void update(UpdateSettingsRequest request) {
        this.studentIdentifier = request.studentIdentifier();
        this.selfRegistrationEnabled = request.selfRegistrationEnabled();
    }
}
