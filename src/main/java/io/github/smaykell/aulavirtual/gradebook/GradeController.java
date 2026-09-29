package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping("/courses/{courseId}/grades")
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_READ + "')")
    public List<GradeResponse> ofCourse(Authentication authentication,
            @PathVariable UUID courseId) {

        return gradeService.ofCourse(authentication.getName(), courseId);
    }
}
