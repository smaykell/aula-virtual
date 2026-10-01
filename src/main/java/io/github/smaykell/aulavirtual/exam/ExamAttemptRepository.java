package io.github.smaykell.aulavirtual.exam;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.id = :id")
    Optional<ExamAttempt> findForUpdate(@Param("id") UUID id);

    List<ExamAttempt> findByExamIdAndStudentIdOrderByNumber(UUID examId, UUID studentId);

    List<ExamAttempt> findByExamIdOrderByStartedAt(UUID examId);

    boolean existsByExamId(UUID examId);

    @Query("""
            select max(a.score) from ExamAttempt a
            where a.examId = :examId and a.studentId = :studentId and a.status = :status
            """)
    Optional<BigDecimal> findBestScore(@Param("examId") UUID examId,
            @Param("studentId") UUID studentId, @Param("status") AttemptStatus status);

    @Query("""
            select a.id from ExamAttempt a
            where a.status = :status and a.deadline <= :now
            """)
    List<UUID> findIdsExpiredAt(@Param("status") AttemptStatus status,
            @Param("now") Instant now);

    @Query("""
            select count(a) > 0 from ExamAttempt a
            where a.examId in (select q.examId from ExamQuestion q where q.questionId = :questionId)
            """)
    boolean existsForQuestion(@Param("questionId") UUID questionId);
}
