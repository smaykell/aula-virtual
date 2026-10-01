package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.exam.dto.ExamData;
import io.github.smaykell.aulavirtual.exam.dto.ExamQuestionData;
import io.github.smaykell.aulavirtual.exam.dto.ExamQuestionResponse;
import io.github.smaykell.aulavirtual.exam.dto.ExamQuestionsData;
import io.github.smaykell.aulavirtual.exam.dto.ExamResponse;
import io.github.smaykell.aulavirtual.exam.exception.ExamHasAttemptsException;
import io.github.smaykell.aulavirtual.exam.exception.ExamHasWorkException;
import io.github.smaykell.aulavirtual.exam.exception.ExamNotFoundException;
import io.github.smaykell.aulavirtual.exam.exception.InvalidExamWindowException;
import io.github.smaykell.aulavirtual.exam.exception.RepeatedQuestionException;
import io.github.smaykell.aulavirtual.exam.question.QuestionService;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.GradingSchemeService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository attemptRepository;
    private final QuestionService questionService;
    private final CourseService courseService;
    private final GradeService gradeService;
    private final GradingSchemeService gradingSchemeService;

    @Transactional(readOnly = true)
    public List<ExamResponse> list(String actorUsername, UUID unitId) {
        courseService.memberOf(actorUsername, courseService.courseOf(unitId));

        List<Exam> exams = examRepository.findByUnitIdOrderByOpensAt(unitId);
        Map<UUID, Long> questionCounts = examQuestionRepository
                .findByExamIdIn(exams.stream().map(Exam::getId).toList()).stream()
                .collect(Collectors.groupingBy(ExamQuestion::getExamId, Collectors.counting()));
        return exams.stream()
                .map(exam -> ExamResponse.from(exam,
                        questionCounts.getOrDefault(exam.getId(), 0L).intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamResponse get(String actorUsername, UUID examId) {
        Exam exam = existing(examId);
        memberFor(actorUsername, exam);
        return responseOf(exam);
    }

    @Transactional
    public ExamResponse create(String actorUsername, UUID unitId, ExamData data) {
        UUID courseId = courseService.courseOf(unitId);
        courseService.requireWritable(actorUsername, courseId);
        requireValid(courseId, data);

        return ExamResponse.from(examRepository.save(Exam.create(unitId, courseId, data)), 0);
    }

    @Transactional
    public ExamResponse update(String actorUsername, UUID examId, ExamData data) {
        Exam exam = writable(actorUsername, examId);
        requireValid(exam.getCourseId(), data);
        exam.update(data);
        return responseOf(exam);
    }

    @Transactional
    public void delete(String actorUsername, UUID examId) {
        Exam exam = writable(actorUsername, examId);
        if (attemptRepository.existsByExamId(examId)
                || gradeService.anyFor(GradeSource.EXAM, examId)) {
            throw new ExamHasWorkException();
        }
        examRepository.delete(exam);
    }

    @Transactional(readOnly = true)
    public List<ExamQuestionResponse> questions(String actorUsername, UUID examId) {
        Exam exam = existing(examId);
        AnswerKey.requireReadableBy(memberFor(actorUsername, exam));
        return questionsOf(examQuestionRepository.findByExamIdOrderByPosition(examId));
    }

    @Transactional
    public List<ExamQuestionResponse> replaceQuestions(String actorUsername, UUID examId,
            ExamQuestionsData data) {

        Exam exam = writable(actorUsername, examId);
        if (attemptRepository.existsByExamId(examId)) {
            throw new ExamHasAttemptsException();
        }
        List<UUID> questionIds = data.questions().stream()
                .map(ExamQuestionData::questionId)
                .toList();
        requireOnce(questionIds);
        questionService.requireInBank(exam.getCourseId(), questionIds);

        examQuestionRepository.deleteByExamId(examId);
        List<ExamQuestion> placed = examQuestionRepository.saveAll(place(examId,
                data.questions()));
        exam.scoreOutOf(placed.stream()
                .map(ExamQuestion::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return questionsOf(placed);
    }

    CourseMember memberFor(String actorUsername, Exam exam) {
        return courseService.memberOf(actorUsername, exam.getCourseId());
    }

    Exam existing(UUID examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException(examId));
    }

    Exam writable(String actorUsername, UUID examId) {
        Exam exam = existing(examId);
        courseService.requireWritable(actorUsername, exam.getCourseId());
        return exam;
    }

    private ExamResponse responseOf(Exam exam) {
        return ExamResponse.from(exam,
                examQuestionRepository.findByExamIdOrderByPosition(exam.getId()).size());
    }

    private List<ExamQuestionResponse> questionsOf(List<ExamQuestion> examQuestions) {
        Map<UUID, QuestionResponse> questions = questionService.byId(
                examQuestions.stream().map(ExamQuestion::getQuestionId).toList());
        return examQuestions.stream()
                .map(placed -> new ExamQuestionResponse(placed.getPosition(), placed.getPoints(),
                        questions.get(placed.getQuestionId())))
                .toList();
    }

    private void requireValid(UUID courseId, ExamData data) {
        if (!data.closesAfterOpening()) {
            throw new InvalidExamWindowException();
        }
        if (data.categoryId() != null) {
            gradingSchemeService.requireCategoryIn(courseId, data.categoryId());
        }
    }

    private static void requireOnce(List<UUID> questionIds) {
        Set<UUID> seen = new HashSet<>();
        questionIds.stream()
                .filter(id -> !seen.add(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new RepeatedQuestionException(id);
                });
    }

    private static List<ExamQuestion> place(UUID examId, List<ExamQuestionData> questions) {
        List<ExamQuestion> placed = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            ExamQuestionData question = questions.get(index);
            placed.add(ExamQuestion.of(examId, question.questionId(), index + 1,
                    question.points()));
        }
        return placed;
    }
}
