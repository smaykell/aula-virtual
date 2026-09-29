package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.gradebook.dto.GradebookResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradebookController {

    private final GradebookService gradebookService;

    @GetMapping("/courses/{courseId}/gradebook")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public GradebookResponse of(Authentication authentication, @PathVariable UUID courseId) {
        return gradebookService.of(authentication.getName(), courseId);
    }
}
