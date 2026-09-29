package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.AttachmentDownloadResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentDownloadService downloadService;

    @GetMapping("/attachments/{id}/$download")
    @PreAuthorize("hasAuthority('" + Permission.Name.ASSIGNMENTS_READ + "')")
    public AttachmentDownloadResponse download(Authentication authentication,
            @PathVariable UUID id) {

        return downloadService.download(authentication.getName(), id);
    }
}
