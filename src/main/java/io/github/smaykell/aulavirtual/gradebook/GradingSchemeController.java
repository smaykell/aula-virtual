package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeData;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradingSchemeController {

    private final GradingSchemeService schemeService;

    @GetMapping("/courses/{courseId}/grading-scheme")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public GradingSchemeResponse get(Authentication authentication,
            @PathVariable UUID courseId) {

        return schemeService.get(authentication.getName(), courseId);
    }

    @PutMapping("/courses/{courseId}/grading-scheme")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public GradingSchemeResponse replace(Authentication authentication,
            @PathVariable UUID courseId, @Valid @RequestBody GradingSchemeData request) {

        return schemeService.replace(authentication.getName(), courseId, request);
    }
}
