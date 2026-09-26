package io.github.smaykell.aulavirtual.course.enrollment;

import io.github.smaykell.aulavirtual.course.enrollment.dto.EnrollmentResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/me/enrollments")
    public List<EnrollmentResponse> mine(Authentication authentication) {
        return enrollmentService.mine(authentication.getName());
    }

    @GetMapping(value = "/me/enrollments", params = "code")
    public List<EnrollmentResponse> mineIn(Authentication authentication,
            @RequestParam String code) {
        return enrollmentService.mineIn(authentication.getName(), code);
    }
}
