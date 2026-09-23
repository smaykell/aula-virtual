package io.github.smaykell.aulavirtual.course.unit;

import io.github.smaykell.aulavirtual.course.unit.dto.MaterialData;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialDownloadResponse;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialResponse;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialUploadRequest;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialUploadResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
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
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping("/units/{unitId}/materials")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public MaterialResponse create(Authentication authentication, @PathVariable UUID unitId,
            @Valid @RequestBody MaterialData request) {

        return materialService.create(authentication.getName(), unitId, request);
    }

    @PostMapping("/units/{unitId}/materials/$upload")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public MaterialUploadResponse prepareUpload(Authentication authentication,
            @PathVariable UUID unitId, @Valid @RequestBody MaterialUploadRequest request) {

        return materialService.prepareUpload(authentication.getName(), unitId, request);
    }

    @PutMapping("/materials/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public MaterialResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody MaterialData request) {

        return materialService.update(authentication.getName(), id, request);
    }

    @GetMapping("/materials/{id}/$download")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public MaterialDownloadResponse download(Authentication authentication,
            @PathVariable UUID id) {

        return materialService.download(authentication.getName(), id);
    }

    @DeleteMapping("/materials/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        materialService.delete(authentication.getName(), id);
    }
}
