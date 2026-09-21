package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.EnrollmentResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.JoinCourseRequest;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/$join")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.ENROLLMENTS_CREATE + "')")
    public EnrollmentResponse join(Authentication authentication,
            @Valid @RequestBody JoinCourseRequest request) {

        return enrollmentService.join(authentication.getName(), request);
    }

    @GetMapping("/courses/{courseId}/enrollments")
    @PreAuthorize("hasAuthority('" + Permission.Name.ENROLLMENTS_READ + "')")
    public PageResponse<EnrollmentResponse> list(Authentication authentication,
            @PathVariable UUID courseId,
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(sort = "requestedAt") Pageable pageable) {

        return enrollmentService.list(authentication.getName(), courseId, status, pageable);
    }

    @PostMapping("/enrollments/{id}/$accept")
    @PreAuthorize("hasAuthority('" + Permission.Name.ENROLLMENTS_UPDATE + "')")
    public EnrollmentResponse accept(Authentication authentication, @PathVariable UUID id) {
        return enrollmentService.accept(authentication.getName(), id);
    }

    @PostMapping("/enrollments/{id}/$reject")
    @PreAuthorize("hasAuthority('" + Permission.Name.ENROLLMENTS_UPDATE + "')")
    public EnrollmentResponse reject(Authentication authentication, @PathVariable UUID id) {
        return enrollmentService.reject(authentication.getName(), id);
    }

    @PostMapping("/enrollments/{id}/$withdraw")
    @PreAuthorize("hasAuthority('" + Permission.Name.ENROLLMENTS_UPDATE + "')")
    public EnrollmentResponse withdraw(Authentication authentication, @PathVariable UUID id) {
        return enrollmentService.withdraw(authentication.getName(), id);
    }
}
