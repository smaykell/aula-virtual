package io.github.smaykell.aulavirtual.assignment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    List<Submission> findByAssignmentIdAndStudentIdIn(UUID assignmentId,
            Collection<UUID> studentIds);

    boolean existsByAssignmentId(UUID assignmentId);

    @Query("select s.studentId from Submission s where s.assignmentId = :assignmentId")
    Set<UUID> findStudentIdsByAssignmentId(@Param("assignmentId") UUID assignmentId);

    Page<Submission> findByAssignmentId(UUID assignmentId, Pageable pageable);

    Page<Submission> findByAssignmentIdAndStatus(UUID assignmentId, SubmissionStatus status,
            Pageable pageable);

    Page<Submission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId,
            Pageable pageable);
}
