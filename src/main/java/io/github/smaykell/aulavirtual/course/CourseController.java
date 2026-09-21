package io.github.smaykell.aulavirtual.course;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.course.dto.CourseResponse;
import io.github.smaykell.aulavirtual.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.course.dto.UpdateCourseRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public PageResponse<CourseResponse> list(Authentication authentication,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) CourseStatus status,
            @PageableDefault(sort = "createdAt") Pageable pageable) {

        return courseService.list(authentication.getName(), teacherId, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public CourseResponse get(Authentication authentication, @PathVariable UUID id) {
        return courseService.get(authentication.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_CREATE + "')")
    public CourseResponse create(Authentication authentication,
            @Valid @RequestBody CreateCourseRequest request) {

        return courseService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public CourseResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateCourseRequest request) {

        return courseService.update(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/$archive")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public CourseResponse archive(Authentication authentication, @PathVariable UUID id) {
        return courseService.archive(authentication.getName(), id);
    }

    @PostMapping("/{id}/$activate")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public CourseResponse activate(Authentication authentication, @PathVariable UUID id) {
        return courseService.activate(authentication.getName(), id);
    }
}
