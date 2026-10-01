package io.github.smaykell.aulavirtual.exam.question;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByCourseIdOrderByCreatedAt(UUID courseId);
}
