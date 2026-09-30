package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.gradebook.dto.GradebookResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradebookController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final String EXPORT_FILE_NAME = "registro-de-notas.csv";

    private final GradebookService gradebookService;

    @GetMapping("/courses/{courseId}/gradebook")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public GradebookResponse of(Authentication authentication, @PathVariable UUID courseId) {
        return gradebookService.of(authentication.getName(), courseId);
    }

    @GetMapping("/courses/{courseId}/gradebook/$export")
    @PreAuthorize("hasAuthority('" + Permission.Name.COURSES_READ + "')")
    public ResponseEntity<byte[]> export(Authentication authentication,
            @PathVariable UUID courseId) {

        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(EXPORT_FILE_NAME).build()
                                .toString())
                .body(gradebookService.export(authentication.getName(), courseId));
    }
}
