package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.AssignmentData;
import io.github.smaykell.aulavirtual.assignment.dto.AssignmentResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping("/units/{unitId}/assignments")
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_READ + "')")
    public List<AssignmentResponse> list(Authentication authentication,
            @PathVariable UUID unitId) {

        return assignmentService.list(authentication.getName(), unitId);
    }

    @PostMapping("/units/{unitId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_CREATE + "')")
    public AssignmentResponse create(Authentication authentication, @PathVariable UUID unitId,
            @Valid @RequestBody AssignmentData request) {

        return assignmentService.create(authentication.getName(), unitId, request);
    }

    @GetMapping("/assignments/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_READ + "')")
    public AssignmentResponse get(Authentication authentication, @PathVariable UUID id) {
        return assignmentService.get(authentication.getName(), id);
    }

    @PutMapping("/assignments/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_UPDATE + "')")
    public AssignmentResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody AssignmentData request) {

        return assignmentService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_UPDATE + "')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        assignmentService.delete(authentication.getName(), id);
    }
}
