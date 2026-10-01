package io.github.smaykell.aulavirtual.exam.dto;

import io.github.smaykell.aulavirtual.exam.Exam;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamResponse(
        UUID id,
        UUID unitId,
        UUID courseId,
        String title,
        String instructions,
        Instant opensAt,
        Instant closesAt,
        Integer timeLimitMinutes,
        int maxAttempts,
        boolean shuffleQuestions,
        boolean shuffleOptions,
        boolean showsAnswers,
        UUID categoryId,
        BigDecimal maxScore,
        int questionCount,
        Instant createdAt) {

    public static ExamResponse from(Exam exam, int questionCount) {
        return new ExamResponse(exam.getId(), exam.getUnitId(), exam.getCourseId(),
                exam.getTitle(), exam.getInstructions(), exam.getOpensAt(), exam.getClosesAt(),
                exam.getTimeLimitMinutes(), exam.getMaxAttempts(), exam.isShuffleQuestions(),
                exam.isShuffleOptions(), exam.isShowsAnswers(), exam.getCategoryId(),
                exam.getMaxScore(), questionCount, exam.getCreatedAt());
    }
}
