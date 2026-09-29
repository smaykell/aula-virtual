package io.github.smaykell.aulavirtual.assignment.attachment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByAssignmentIdOrderByPosition(UUID assignmentId);

    List<Attachment> findBySubmissionIdOrderByPosition(UUID submissionId);

    List<Attachment> findByAssignmentIdInOrderByPosition(Collection<UUID> assignmentIds);

    List<Attachment> findBySubmissionIdInOrderByPosition(Collection<UUID> submissionIds);

    boolean existsByStorageKey(String storageKey);
}
