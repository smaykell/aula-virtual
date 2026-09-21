package io.github.smaykell.aulavirtual.modules.teacher;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.modules.teacher.dto.CreateTeacherRequest;
import io.github.smaykell.aulavirtual.modules.teacher.dto.TeacherResponse;
import io.github.smaykell.aulavirtual.modules.teacher.dto.UpdateTeacherRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.ChangePasswordRequest;
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
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.Name.TEACHERS_READ + "')")
    public PageResponse<TeacherResponse> list(Authentication authentication,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt") Pageable pageable) {

        return teacherService.list(authentication.getName(), active, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.TEACHERS_READ + "')")
    public TeacherResponse get(Authentication authentication, @PathVariable UUID id) {
        return teacherService.get(authentication.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.TEACHERS_CREATE + "')")
    public TeacherResponse create(Authentication authentication,
            @Valid @RequestBody CreateTeacherRequest request) {

        return teacherService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.TEACHERS_UPDATE + "')")
    public TeacherResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateTeacherRequest request) {

        return teacherService.update(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/$enable")
    @PreAuthorize("hasAuthority('" + Permission.Name.TEACHERS_UPDATE + "')")
    public TeacherResponse enable(Authentication authentication, @PathVariable UUID id) {
        return teacherService.enable(authentication.getName(), id);
    }

    @PostMapping("/{id}/$disable")
    @PreAuthorize("hasAuthority('" + Permission.Name.TEACHERS_UPDATE + "')")
    public TeacherResponse disable(Authentication authentication, @PathVariable UUID id) {
        return teacherService.disable(authentication.getName(), id);
    }

    @PostMapping("/{id}/$changePassword")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.TEACHERS_UPDATE + "')")
    public void changePassword(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {

        teacherService.changePassword(authentication.getName(), id, request);
    }
}
