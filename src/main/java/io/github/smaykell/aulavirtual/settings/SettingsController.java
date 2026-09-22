package io.github.smaykell.aulavirtual.settings;

import io.github.smaykell.aulavirtual.security.Permission;
import io.github.smaykell.aulavirtual.settings.dto.SettingsResponse;
import io.github.smaykell.aulavirtual.settings.dto.UpdateSettingsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.Name.SETTINGS_READ + "')")
    public SettingsResponse get() {
        return settingsService.current();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('" + Permission.Name.SETTINGS_UPDATE + "')")
    public SettingsResponse update(@Valid @RequestBody UpdateSettingsRequest request) {
        return settingsService.update(request);
    }
}
