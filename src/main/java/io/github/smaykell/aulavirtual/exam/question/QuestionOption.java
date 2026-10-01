package io.github.smaykell.aulavirtual.exam.question;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionOption extends BaseEntity {

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "position", nullable = false, updatable = false)
    private int position;

    @Column(name = "text", nullable = false, length = 1000, updatable = false)
    private String text;

    @Column(name = "correct", nullable = false, updatable = false)
    private boolean correct;

    private QuestionOption(UUID questionId, int position, OptionData data) {
        this.questionId = questionId;
        this.position = position;
        this.text = data.text().trim();
        this.correct = data.correct();
    }

    public static QuestionOption of(UUID questionId, int position, OptionData data) {
        return new QuestionOption(questionId, position, data);
    }
}
