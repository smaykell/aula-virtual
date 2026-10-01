package io.github.smaykell.aulavirtual.exam;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    List<Exam> findByUnitIdOrderByOpensAt(UUID unitId);

    List<Exam> findByCourseIdAndMaxScoreGreaterThanOrderByClosesAt(UUID courseId,
            BigDecimal maxScore);
}
