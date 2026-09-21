package io.github.smaykell.aulavirtual.student;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import io.github.smaykell.aulavirtual.student.dto.CreateStudentRequest;
import io.github.smaykell.aulavirtual.student.dto.StudentResponse;
import io.github.smaykell.aulavirtual.student.dto.UpdateStudentRequest;
import io.github.smaykell.aulavirtual.user.dto.ChangePasswordRequest;
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
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.Name.STUDENTS_READ + "')")
    public PageResponse<StudentResponse> list(Authentication authentication,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt") Pageable pageable) {

        return studentService.list(authentication.getName(), active, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.STUDENTS_READ + "')")
    public StudentResponse get(Authentication authentication, @PathVariable UUID id) {
        return studentService.get(authentication.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.STUDENTS_CREATE + "')")
    public StudentResponse create(Authentication authentication,
            @Valid @RequestBody CreateStudentRequest request) {

        return studentService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.STUDENTS_UPDATE + "')")
    public StudentResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest request) {

        return studentService.update(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/$enable")
    @PreAuthorize("hasAuthority('" + Permission.Name.STUDENTS_UPDATE + "')")
    public StudentResponse enable(Authentication authentication, @PathVariable UUID id) {
        return studentService.enable(authentication.getName(), id);
    }

    @PostMapping("/{id}/$disable")
    @PreAuthorize("hasAuthority('" + Permission.Name.STUDENTS_UPDATE + "')")
    public StudentResponse disable(Authentication authentication, @PathVariable UUID id) {
        return studentService.disable(authentication.getName(), id);
    }

    @PostMapping("/{id}/$changePassword")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.STUDENTS_UPDATE + "')")
    public void changePassword(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {

        studentService.changePassword(authentication.getName(), id, request);
    }
}
