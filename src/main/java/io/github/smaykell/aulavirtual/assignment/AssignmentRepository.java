package io.github.smaykell.aulavirtual.assignment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByUnitIdOrderByDueAt(UUID unitId);

    List<Assignment> findByCourseIdOrderByDueAt(UUID courseId);

    List<Assignment> findByRemindedAtIsNullAndDueAtBetween(Instant from, Instant to);

    @Query("""
            select a from Assignment a
            where a.courseId in :courseIds
              and (a.dueAt >= :now or (a.allowsLate = true and a.dueAt >= :lateSince))
              and not exists (
                  select s.id from Submission s
                  where s.assignmentId = a.id and s.studentId = :studentId)
            order by a.dueAt
            """)
    List<Assignment> findPendingFor(@Param("studentId") UUID studentId,
            @Param("courseIds") Collection<UUID> courseIds, @Param("now") Instant now,
            @Param("lateSince") Instant lateSince, Limit limit);
}
