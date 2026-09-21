package io.github.smaykell.aulavirtual.modules.course;

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
            """)
    Page<Course> search(@Param("teacherId") UUID teacherId,
            @Param("status") CourseStatus status, Pageable pageable);

    boolean existsByInvitationCode(String invitationCode);
}
