package io.github.smaykell.aulavirtual.settings;

import io.github.smaykell.aulavirtual.settings.dto.SettingsResponse;
import io.github.smaykell.aulavirtual.settings.dto.UpdateSettingsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;

    @Transactional(readOnly = true)
    public SettingsResponse current() {
        return SettingsResponse.from(single());
    }

    @Transactional
    public SettingsResponse update(UpdateSettingsRequest request) {
        Settings settings = single();
        settings.update(request);
        return SettingsResponse.from(settings);
    }

    private Settings single() {
        return settingsRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "La fila de configuracion la siembra V12 y falta en la base"));
    }
}
