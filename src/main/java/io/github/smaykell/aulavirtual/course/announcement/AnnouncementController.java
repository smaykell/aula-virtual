package io.github.smaykell.aulavirtual.course.announcement;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementData;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/courses/{courseId}/announcements")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public PageResponse<AnnouncementResponse> list(Authentication authentication,
            @PathVariable UUID courseId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return announcementService.list(authentication.getName(), courseId, pageable);
    }

    @PostMapping("/courses/{courseId}/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public AnnouncementResponse publish(Authentication authentication,
            @PathVariable UUID courseId, @Valid @RequestBody AnnouncementData request) {

        return announcementService.publish(authentication.getName(), courseId, request);
    }

    @PutMapping("/announcements/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public AnnouncementResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody AnnouncementData request) {

        return announcementService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/announcements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_UPDATE + "')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        announcementService.delete(authentication.getName(), id);
    }
}
