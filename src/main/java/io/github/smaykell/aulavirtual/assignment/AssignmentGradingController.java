package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.GradeData;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AssignmentGradingController {

    private final AssignmentGradingService gradingService;

    @PutMapping("/assignments/{id}/grades/{studentId}")
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_UPDATE + "')")
    public GradeResponse grade(Authentication authentication, @PathVariable UUID id,
            @PathVariable UUID studentId, @Valid @RequestBody GradeData request) {

        return gradingService.grade(authentication.getName(), id, studentId, request);
    }
}
