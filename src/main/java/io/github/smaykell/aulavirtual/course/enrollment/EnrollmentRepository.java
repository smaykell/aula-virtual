package io.github.smaykell.aulavirtual.course.enrollment;

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

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByCourseIdAndStudentId(UUID courseId, UUID studentId);

    boolean existsByCourseIdAndStudentIdAndStatus(UUID courseId, UUID studentId,
            EnrollmentStatus status);

    boolean existsByCourseIdAndStudentIdAndStatusIn(UUID courseId, UUID studentId,
            Collection<EnrollmentStatus> statuses);

    @Query("""
            select e.courseId from Enrollment e
            where e.studentId = :studentId
              and e.status = :status
              and e.courseId in :courseIds
            """)
    Set<UUID> findCourseIdsAmong(@Param("studentId") UUID studentId,
            @Param("status") EnrollmentStatus status,
            @Param("courseIds") Collection<UUID> courseIds);

    @Query("""
            select e.studentId from Enrollment e
            where e.courseId = :courseId
              and e.status = :status
            """)
    List<UUID> findStudentIdsByCourseIdAndStatus(@Param("courseId") UUID courseId,
            @Param("status") EnrollmentStatus status);

    Page<Enrollment> findByCourseId(UUID courseId, Pageable pageable);

    Page<Enrollment> findByCourseIdAndStatus(UUID courseId, EnrollmentStatus status,
            Pageable pageable);

    List<Enrollment> findByStudentIdOrderByRequestedAtDesc(UUID studentId);
}
