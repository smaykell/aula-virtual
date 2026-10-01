package io.github.smaykell.aulavirtual.exam.question;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseEntity {

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionType type;

    @Column(name = "statement", nullable = false, length = 4000)
    private String statement;

    @Column(name = "model_answer", length = 4000)
    private String modelAnswer;

    private Question(UUID courseId, QuestionData data) {
        this.courseId = courseId;
        update(data);
    }

    public static Question create(UUID courseId, QuestionData data) {
        return new Question(courseId, data);
    }

    public final void update(QuestionData data) {
        this.type = data.type();
        this.statement = data.statement().trim();
        this.modelAnswer = trimmed(data.modelAnswer());
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
