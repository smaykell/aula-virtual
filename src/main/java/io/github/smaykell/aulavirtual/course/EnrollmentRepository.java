package io.github.smaykell.aulavirtual.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByCourseIdAndStudentId(UUID courseId, UUID studentId);

    boolean existsByCourseIdAndStudentIdAndStatus(UUID courseId, UUID studentId,
            EnrollmentStatus status);

    Page<Enrollment> findByCourseId(UUID courseId, Pageable pageable);

    Page<Enrollment> findByCourseIdAndStatus(UUID courseId, EnrollmentStatus status,
            Pageable pageable);

    List<Enrollment> findByStudentIdOrderByRequestedAtDesc(UUID studentId);
}
