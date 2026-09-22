package io.github.smaykell.aulavirtual.course.enrollment;

import io.github.smaykell.aulavirtual.course.enrollment.dto.CourseInvitationResponse;
import io.github.smaykell.aulavirtual.course.enrollment.dto.SelfRegistrationRequest;
import io.github.smaykell.aulavirtual.course.enrollment.dto.SelfRegistrationResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Sin @PreAuthorize y a proposito, como /me/*: son rutas publicas de PUBLIC_PATHS y la
// condicion la pone SecurityConfig. Quien todavia no tiene cuenta no tiene authorities.
@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@SecurityRequirements
public class InvitationController {

    private final SelfEnrollmentService selfEnrollmentService;

    @GetMapping("/{code}")
    public CourseInvitationResponse preview(@PathVariable String code) {
        return selfEnrollmentService.preview(code);
    }

    @PostMapping("/{code}/$register")
    @ResponseStatus(HttpStatus.CREATED)
    public SelfRegistrationResponse register(@PathVariable String code,
            @Valid @RequestBody SelfRegistrationRequest request) {

        return selfEnrollmentService.register(code, request);
    }
}
