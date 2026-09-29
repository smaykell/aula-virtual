package io.github.smaykell.aulavirtual.assignment.attachment;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.common.storage.StoredObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment extends BaseEntity {

    @Column(name = "assignment_id", updatable = false)
    private UUID assignmentId;

    @Column(name = "submission_id", updatable = false)
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10, updatable = false)
    private AttachmentKind kind;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "storage_key", length = 255, updatable = false)
    private String storageKey;

    @Column(name = "external_url", length = 2048, updatable = false)
    private String externalUrl;

    @Column(name = "content_type", length = 150, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", updatable = false)
    private Long sizeBytes;

    @Column(name = "position", nullable = false)
    private int position;

    private Attachment(AttachmentOwner owner, AttachmentKind kind, String title, int position) {
        this.assignmentId = owner.assignmentId();
        this.submissionId = owner.submissionId();
        this.kind = kind;
        place(title, position);
    }

    public static Attachment file(AttachmentOwner owner, String title, String storageKey,
            StoredObject stored, int position) {

        Attachment attachment = new Attachment(owner, AttachmentKind.FILE, title, position);
        attachment.storageKey = storageKey;
        attachment.contentType = stored.contentType();
        attachment.sizeBytes = stored.size();
        return attachment;
    }

    public static Attachment link(AttachmentOwner owner, String title, String externalUrl,
            int position) {

        Attachment attachment = new Attachment(owner, AttachmentKind.LINK, title, position);
        attachment.externalUrl = externalUrl;
        return attachment;
    }

    public final void place(String title, int position) {
        this.title = title;
        this.position = position;
    }

    public Optional<String> file() {
        return Optional.ofNullable(storageKey);
    }

    public boolean belongsToAnAssignment() {
        return assignmentId != null;
    }
}
