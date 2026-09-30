package io.github.smaykell.aulavirtual.administrator;

import io.github.smaykell.aulavirtual.administrator.dto.AdministratorResponse;
import io.github.smaykell.aulavirtual.administrator.dto.CreateAdministratorRequest;
import io.github.smaykell.aulavirtual.administrator.dto.UpdateAdministratorRequest;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.security.Permission;
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
@RequestMapping("/administrators")
@RequiredArgsConstructor
public class AdministratorController {

    private final AdministratorService administratorService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.Name.ADMINISTRATORS_READ + "')")
    public PageResponse<AdministratorResponse> list(Authentication authentication,
            @RequestParam(required = false) Boolean active,
            @RequestParam(name = "q", required = false) String search,
            @PageableDefault(sort = "createdAt") Pageable pageable) {

        return administratorService.list(authentication.getName(), active, search, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.ADMINISTRATORS_READ + "')")
    public AdministratorResponse get(Authentication authentication, @PathVariable UUID id) {
        return administratorService.get(authentication.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.ADMINISTRATORS_CREATE + "')")
    public AdministratorResponse create(Authentication authentication,
            @Valid @RequestBody CreateAdministratorRequest request) {

        return administratorService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.ADMINISTRATORS_UPDATE + "')")
    public AdministratorResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateAdministratorRequest request) {

        return administratorService.update(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/$enable")
    @PreAuthorize("hasAuthority('" + Permission.Name.ADMINISTRATORS_UPDATE + "')")
    public AdministratorResponse enable(Authentication authentication, @PathVariable UUID id) {
        return administratorService.enable(authentication.getName(), id);
    }

    @PostMapping("/{id}/$disable")
    @PreAuthorize("hasAuthority('" + Permission.Name.ADMINISTRATORS_UPDATE + "')")
    public AdministratorResponse disable(Authentication authentication, @PathVariable UUID id) {
        return administratorService.disable(authentication.getName(), id);
    }

    @PostMapping("/{id}/$changePassword")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.ADMINISTRATORS_UPDATE + "')")
    public void changePassword(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {

        administratorService.changePassword(authentication.getName(), id, request);
    }
}
