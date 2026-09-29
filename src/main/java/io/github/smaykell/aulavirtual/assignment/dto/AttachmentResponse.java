package io.github.smaykell.aulavirtual.assignment.dto;

import io.github.smaykell.aulavirtual.assignment.attachment.Attachment;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentKind;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        AttachmentKind kind,
        String title,
        String externalUrl,
        String contentType,
        Long size) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getKind(),
                attachment.getTitle(), attachment.getExternalUrl(), attachment.getContentType(),
                attachment.getSizeBytes());
    }
}
