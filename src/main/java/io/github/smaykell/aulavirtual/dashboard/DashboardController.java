package io.github.smaykell.aulavirtual.dashboard;

import io.github.smaykell.aulavirtual.dashboard.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me/dashboard")
    public DashboardResponse get(Authentication authentication) {
        return dashboardService.of(authentication.getName());
    }
}
