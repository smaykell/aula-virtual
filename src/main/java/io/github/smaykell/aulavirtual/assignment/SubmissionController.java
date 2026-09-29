package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.SubmissionData;
import io.github.smaykell.aulavirtual.assignment.dto.SubmissionResponse;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/assignments/{id}/$submit")
    @PreAuthorize("hasAuthority('" + Permission.Name.SUBMISSIONS_CREATE + "')")
    public SubmissionResponse submit(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody SubmissionData request) {

        return submissionService.submit(authentication.getName(), id, request);
    }

    @GetMapping("/assignments/{id}/submissions")
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_READ + "')")
    public PageResponse<SubmissionResponse> list(Authentication authentication,
            @PathVariable UUID id,
            @RequestParam(required = false) SubmissionStatus status,
            @PageableDefault(sort = "submittedAt") Pageable pageable) {

        return submissionService.list(authentication.getName(), id, status, pageable);
    }
}
