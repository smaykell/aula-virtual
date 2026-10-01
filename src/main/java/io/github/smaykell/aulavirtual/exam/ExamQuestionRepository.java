package io.github.smaykell.aulavirtual.exam;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, UUID> {

    List<ExamQuestion> findByExamIdOrderByPosition(UUID examId);

    List<ExamQuestion> findByExamIdIn(Collection<UUID> examIds);

    boolean existsByQuestionId(UUID questionId);

    boolean existsByExamIdAndQuestionId(UUID examId, UUID questionId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ExamQuestion q where q.examId = :examId")
    void deleteByExamId(@Param("examId") UUID examId);
}
