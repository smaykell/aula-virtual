package io.github.smaykell.aulavirtual.course;

import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query("""
            select c from Course c
            where (:teacherId is null or c.teacherId = :teacherId)
              and (:status is null or c.status = :status)
              and function('unaccent' as String, lower(c.name))
                  like function('unaccent' as String, :name)
            """)
    Page<Course> search(@Param("teacherId") UUID teacherId,
            @Param("status") CourseStatus status, @Param("name") String namePattern,
            Pageable pageable);

    @Query(value = """
            select c from Course c
            join Enrollment e on e.courseId = c.id
            where e.studentId = :studentId
              and e.status = :enrollmentStatus
              and (:status is null or c.status = :status)
              and function('unaccent' as String, lower(c.name))
                  like function('unaccent' as String, :name)
            """,
            countQuery = """
            select count(c) from Course c
            join Enrollment e on e.courseId = c.id
            where e.studentId = :studentId
              and e.status = :enrollmentStatus
              and (:status is null or c.status = :status)
              and function('unaccent' as String, lower(c.name))
                  like function('unaccent' as String, :name)
            """)
    Page<Course> searchEnrolled(@Param("studentId") UUID studentId,
            @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus,
            @Param("status") CourseStatus status, @Param("name") String namePattern,
            Pageable pageable);

    Optional<Course> findByInvitationCode(String invitationCode);

    boolean existsByInvitationCode(String invitationCode);
}
