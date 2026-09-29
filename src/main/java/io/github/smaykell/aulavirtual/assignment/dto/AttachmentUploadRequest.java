package io.github.smaykell.aulavirtual.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AttachmentUploadRequest(
        @NotBlank(message = AssignmentConstraints.FILE_NAME_REQUIRED)
        @Size(max = AssignmentConstraints.ATTACHMENT_TITLE_MAX,
                message = AssignmentConstraints.ATTACHMENT_TITLE_TOO_LONG)
        String fileName,

        @NotBlank(message = AssignmentConstraints.CONTENT_TYPE_REQUIRED)
        String contentType,

        @NotNull(message = AssignmentConstraints.SIZE_REQUIRED)
        @Positive(message = AssignmentConstraints.SIZE_POSITIVE)
        Long size) {
}
