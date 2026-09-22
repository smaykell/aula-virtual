package io.github.smaykell.aulavirtual.settings;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, UUID> {
}
