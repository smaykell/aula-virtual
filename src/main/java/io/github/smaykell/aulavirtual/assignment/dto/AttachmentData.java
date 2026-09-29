package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AttachmentData(
        @NotNull(message = AssignmentConstraints.ATTACHMENT_KIND_REQUIRED)
        AttachmentKind kind,

        @Size(max = AssignmentConstraints.ATTACHMENT_TITLE_MAX,
                message = AssignmentConstraints.ATTACHMENT_TITLE_TOO_LONG)
        String title,

        @Size(max = AssignmentConstraints.STORAGE_KEY_MAX,
                message = AssignmentConstraints.STORAGE_KEY_TOO_LONG)
        String storageKey,

        @Size(max = AssignmentConstraints.EXTERNAL_URL_MAX,
                message = AssignmentConstraints.EXTERNAL_URL_TOO_LONG)
        @Pattern(regexp = AssignmentConstraints.WEB_ADDRESS,
                message = AssignmentConstraints.EXTERNAL_URL_INVALID)
        String externalUrl) {
}
