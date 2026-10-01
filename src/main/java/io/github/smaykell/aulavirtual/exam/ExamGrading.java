package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeEntry;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ExamGrading {

    private final ExamAttemptRepository attemptRepository;
    private final GradeService gradeService;

    void recordBestAutomatically(Exam exam, UUID studentId) {
        bestOf(exam, studentId).ifPresent(gradeService::recordAutomatic);
    }

    void recordBestBy(String graderUsername, Exam exam, UUID studentId) {
        bestOf(exam, studentId).ifPresent(entry -> gradeService.record(graderUsername, entry));
    }

    boolean isReturnedTo(Exam exam, UUID studentId) {
        return gradeService.returnedTo(GradeSource.EXAM, exam.getId(), studentId);
    }

    private Optional<GradeEntry> bestOf(Exam exam, UUID studentId) {
        return attemptRepository
                .findBestScore(exam.getId(), studentId, AttemptStatus.GRADED)
                .map(best -> entryFor(exam, studentId, best));
    }

    private static GradeEntry entryFor(Exam exam, UUID studentId, BigDecimal score) {
        return new GradeEntry(GradeSource.EXAM, exam.getId(), exam.getCourseId(), studentId,
                exam.getMaxScore(), score, null);
    }
}
