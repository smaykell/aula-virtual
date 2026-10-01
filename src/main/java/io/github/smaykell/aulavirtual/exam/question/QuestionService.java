package io.github.smaykell.aulavirtual.exam.question;

import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.exam.AnswerKey;
import io.github.smaykell.aulavirtual.exam.ExamQuestionRepository;
import io.github.smaykell.aulavirtual.exam.exception.QuestionInUseException;
import io.github.smaykell.aulavirtual.exam.exception.QuestionNotFoundException;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionData;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionData;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final CourseService courseService;

    @Transactional(readOnly = true)
    public List<QuestionResponse> list(String actorUsername, UUID courseId) {
        AnswerKey.requireReadableBy(courseService.memberOf(actorUsername, courseId));
        return responsesOf(questionRepository.findByCourseIdOrderByCreatedAt(courseId));
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(String actorUsername, UUID questionId) {
        Question question = existing(questionId);
        AnswerKey.requireReadableBy(courseService.memberOf(actorUsername,
                question.getCourseId()));
        return responsesOf(List.of(question)).getFirst();
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
        Question question = writable(actorUsername, questionId);
        if (examQuestionRepository.existsByQuestionId(questionId)) {
            throw new QuestionInUseException();
        }
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public void requireInBank(UUID courseId, Collection<UUID> questionIds) {
        Set<UUID> inBank = questionRepository.findByCourseIdAndIdIn(courseId, questionIds)
                .stream()
                .map(Question::getId)
                .collect(Collectors.toSet());
        questionIds.stream()
                .filter(id -> !inBank.contains(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new QuestionNotFoundException(id);
                });
    }

    @Transactional(readOnly = true)
    public Map<UUID, QuestionResponse> byId(Collection<UUID> questionIds) {
        return responsesOf(questionRepository.findAllById(questionIds)).stream()
                .collect(Collectors.toMap(QuestionResponse::id, Function.identity()));
    }

    private List<QuestionResponse> responsesOf(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<QuestionOption>> options = optionRepository
                .findByQuestionIdInOrderByPosition(questions.stream().map(Question::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        return questions.stream()
                .map(question -> QuestionResponse.from(question,
                        options.getOrDefault(question.getId(), List.of())))
                .toList();
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

    private List<QuestionOption> place(Question question, List<OptionData> options) {
        List<QuestionOption> placed = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            placed.add(QuestionOption.of(question.getId(), index + 1, options.get(index)));
        }
        return optionRepository.saveAll(placed);
    }
}
