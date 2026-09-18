package io.github.smaykell.aulavirtual.modules.user;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.modules.user.dto.CreateUserRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.UserResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.Name.USERS_READ + "')")
    public PageResponse<UserResponse> list(Authentication authentication,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "username") Pageable pageable) {

        return userService.list(authentication.getName(), active, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.USERS_CREATE + "')")
    public UserResponse create(Authentication authentication,
            @Valid @RequestBody CreateUserRequest request) {

        return userService.create(authentication.getName(), request);
    }

    @PostMapping("/{id}/$enable")
    @PreAuthorize("hasAuthority('" + Permission.Name.USERS_UPDATE + "')")
    public UserResponse enable(Authentication authentication, @PathVariable UUID id) {
        return userService.enable(authentication.getName(), id);
    }

    @PostMapping("/{id}/$disable")
    @PreAuthorize("hasAuthority('" + Permission.Name.USERS_UPDATE + "')")
    public UserResponse disable(Authentication authentication, @PathVariable UUID id) {
        return userService.disable(authentication.getName(), id);
    }
}
