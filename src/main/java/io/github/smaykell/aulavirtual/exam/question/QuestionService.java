package io.github.smaykell.aulavirtual.exam.question;

import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.exam.exception.BankRequiresStaffException;
import io.github.smaykell.aulavirtual.exam.exception.QuestionNotFoundException;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionData;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionData;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final CourseService courseService;

    @Transactional(readOnly = true)
    public List<QuestionResponse> list(String actorUsername, UUID courseId) {
        requireStaff(courseService.memberOf(actorUsername, courseId));

        List<Question> questions = questionRepository.findByCourseIdOrderByCreatedAt(courseId);
        Map<UUID, List<QuestionOption>> options = optionsOf(
                questions.stream().map(Question::getId).toList());
        return questions.stream()
                .map(question -> QuestionResponse.from(question,
                        options.getOrDefault(question.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(String actorUsername, UUID questionId) {
        Question question = existing(questionId);
        requireStaff(courseService.memberOf(actorUsername, question.getCourseId()));
        return responseOf(question);
    }

    @Transactional
    public QuestionResponse create(String actorUsername, UUID courseId, QuestionData data) {
        courseService.requireWritable(actorUsername, courseId);
        List<OptionData> options = QuestionShape.optionsFor(data);

        Question question = questionRepository.save(Question.create(courseId, data));
        return QuestionResponse.from(question, place(question, options));
    }

    @Transactional
    public QuestionResponse update(String actorUsername, UUID questionId, QuestionData data) {
        Question question = writable(actorUsername, questionId);
        List<OptionData> options = QuestionShape.optionsFor(data);

        question.update(data);
        optionRepository.deleteByQuestionId(question.getId());
        return QuestionResponse.from(question, place(question, options));
    }

    @Transactional
    public void delete(String actorUsername, UUID questionId) {
        questionRepository.delete(writable(actorUsername, questionId));
    }

    Map<UUID, List<QuestionOption>> optionsOf(Collection<UUID> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findByQuestionIdInOrderByPosition(questionIds).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
    }

    private Question writable(String actorUsername, UUID questionId) {
        Question question = existing(questionId);
        courseService.requireWritable(actorUsername, question.getCourseId());
        return question;
    }

    private Question existing(UUID questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }

    private QuestionResponse responseOf(Question question) {
        return QuestionResponse.from(question,
                optionsOf(List.of(question.getId())).getOrDefault(question.getId(), List.of()));
    }

    private List<QuestionOption> place(Question question, List<OptionData> options) {
        List<QuestionOption> placed = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            placed.add(QuestionOption.of(question.getId(), index + 1, options.get(index)));
        }
        return optionRepository.saveAll(placed);
    }

    private static void requireStaff(CourseMember member) {
        if (!member.staff()) {
            throw new BankRequiresStaffException();
        }
    }
}
