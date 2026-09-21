package io.github.smaykell.aulavirtual.modules.assignment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    Page<Submission> findByAssignmentId(UUID assignmentId, Pageable pageable);

    Page<Submission> findByAssignmentIdAndStatus(UUID assignmentId, SubmissionStatus status,
            Pageable pageable);

    Page<Submission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId,
            Pageable pageable);
}
