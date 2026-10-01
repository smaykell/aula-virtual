package io.github.smaykell.aulavirtual.exam.question;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {

    List<QuestionOption> findByQuestionIdInOrderByPosition(Collection<UUID> questionIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from QuestionOption o where o.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") UUID questionId);
}
