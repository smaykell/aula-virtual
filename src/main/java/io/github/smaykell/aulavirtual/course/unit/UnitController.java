package io.github.smaykell.aulavirtual.course.unit;

import io.github.smaykell.aulavirtual.course.unit.dto.ReorderUnitsRequest;
import io.github.smaykell.aulavirtual.course.unit.dto.UnitData;
import io.github.smaykell.aulavirtual.course.unit.dto.UnitResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.List;
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
public class UnitController {

    private final UnitService unitService;

    @GetMapping("/courses/{courseId}/units")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public List<UnitResponse> list(Authentication authentication, @PathVariable UUID courseId) {
        return unitService.list(authentication.getName(), courseId);
    }

    @PostMapping("/courses/{courseId}/units")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public UnitResponse create(Authentication authentication, @PathVariable UUID courseId,
            @Valid @RequestBody UnitData request) {

        return unitService.create(authentication.getName(), courseId, request);
    }

    @PostMapping("/courses/{courseId}/units/$reorder")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public List<UnitResponse> reorder(Authentication authentication, @PathVariable UUID courseId,
            @Valid @RequestBody ReorderUnitsRequest request) {

        return unitService.reorder(authentication.getName(), courseId, request);
    }

    @GetMapping("/units/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public UnitResponse get(Authentication authentication, @PathVariable UUID id) {
        return unitService.get(authentication.getName(), id);
    }

    @PutMapping("/units/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public UnitResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UnitData request) {

        return unitService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/units/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        unitService.delete(authentication.getName(), id);
    }
}
