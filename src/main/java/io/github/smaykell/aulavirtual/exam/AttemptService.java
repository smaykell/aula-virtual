package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.exam.dto.AnswerData;
import io.github.smaykell.aulavirtual.exam.dto.AttemptResponse;
import io.github.smaykell.aulavirtual.exam.dto.AttemptSummary;
import io.github.smaykell.aulavirtual.exam.exception.AttemptClosedException;
import io.github.smaykell.aulavirtual.exam.exception.AttemptNotFoundException;
import io.github.smaykell.aulavirtual.exam.exception.AttemptOutOfReachException;
import io.github.smaykell.aulavirtual.exam.exception.ExamNotOpenException;
import io.github.smaykell.aulavirtual.exam.exception.ExamWithoutQuestionsException;
import io.github.smaykell.aulavirtual.exam.exception.GradeReturnedException;
import io.github.smaykell.aulavirtual.exam.exception.NoAttemptsLeftException;
import io.github.smaykell.aulavirtual.exam.exception.OnlyStudentsTakeException;
import io.github.smaykell.aulavirtual.exam.exception.QuestionNotFoundException;
import io.github.smaykell.aulavirtual.exam.question.QuestionService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final ExamService examService;
    private final ExamAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionService questionService;
    private final ExamGrading grading;
    private final Clock clock;

    @Transactional
    public AttemptResponse start(String actorUsername, UUID examId) {
        Exam exam = examService.existing(examId);
        UUID studentId = requireStudent(examService.memberFor(actorUsername, exam));
        Instant now = clock.instant();

        List<ExamAttempt> taken = attemptRepository
                .findByExamIdAndStudentIdOrderByNumber(examId, studentId);
        taken.forEach(attempt -> closeIfExpired(exam, attempt, now));
        ExamAttempt attempt = taken.stream()
                .filter(ExamAttempt::isInProgress)
                .findFirst()
                .orElseGet(() -> begin(exam, studentId, taken.size(), now));
        return paperOf(exam, attempt).disclosed(Disclosure.PAPER);
    }

    @Transactional
    public List<AttemptSummary> list(String actorUsername, UUID examId) {
        Exam exam = examService.existing(examId);
        CourseMember member = examService.memberFor(actorUsername, exam);
        Instant now = clock.instant();

        List<ExamAttempt> attempts = member.staff()
                ? attemptRepository.findByExamIdOrderByStartedAt(examId)
                : attemptRepository.findByExamIdAndStudentIdOrderByNumber(examId,
                        member.studentId());
        attempts.forEach(attempt -> closeIfExpired(exam, attempt, now));
        return attempts.stream()
                .map(attempt -> summaryOf(attempt, disclosureFor(member, exam, attempt)))
                .toList();
    }

    @Transactional
    public AttemptResponse get(String actorUsername, UUID attemptId) {
        ExamAttempt attempt = existing(attemptId);
        Exam exam = examService.existing(attempt.getExamId());
        CourseMember member = readable(actorUsername, exam, attempt);

        closeIfExpired(exam, attempt, clock.instant());
        return paperOf(exam, attempt).disclosed(disclosureFor(member, exam, attempt));
    }

    @Transactional
    public void answer(String actorUsername, UUID attemptId, UUID questionId, AnswerData data) {
        ExamAttempt attempt = locked(attemptId);
        Exam exam = examService.existing(attempt.getExamId());
        requireOwn(actorUsername, exam, attempt);
        if (!attempt.acceptsAnswersAt(clock.instant())) {
            throw new AttemptClosedException();
        }
        if (!examQuestionRepository.existsByExamIdAndQuestionId(exam.getId(), questionId)) {
            throw new QuestionNotFoundException(questionId);
        }

        AttemptAnswer answer = answerRepository
                .findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> AttemptAnswer.of(attemptId, questionId));
        AnswerShape.fill(answer, questionService.byId(List.of(questionId)).get(questionId),
                data);
        answerRepository.save(answer);
    }

    @Transactional
    public AttemptResponse submit(String actorUsername, UUID attemptId) {
        ExamAttempt attempt = locked(attemptId);
        Exam exam = examService.existing(attempt.getExamId());
        CourseMember member = requireOwn(actorUsername, exam, attempt);
        if (!attempt.isInProgress()) {
            throw new AttemptClosedException();
        }

        close(exam, attempt, clock.instant());
        return paperOf(exam, attempt).disclosed(disclosureFor(member, exam, attempt));
    }

    @Transactional
    public void closeExpired(UUID attemptId) {
        ExamAttempt attempt = locked(attemptId);
        closeIfExpired(examService.existing(attempt.getExamId()), attempt, clock.instant());
    }

    private ExamAttempt begin(Exam exam, UUID studentId, int taken, Instant now) {
        requireAvailable(exam, studentId, taken, now);
        return attemptRepository.save(ExamAttempt.start(exam, studentId, taken + 1, now,
                ThreadLocalRandom.current().nextLong()));
    }

    private void requireAvailable(Exam exam, UUID studentId, int taken, Instant now) {
        if (!exam.isOpenAt(now)) {
            throw new ExamNotOpenException();
        }
        if (!exam.hasQuestions()) {
            throw new ExamWithoutQuestionsException();
        }
        if (taken >= exam.getMaxAttempts()) {
            throw new NoAttemptsLeftException();
        }
        if (grading.isReturnedTo(exam, studentId)) {
            throw new GradeReturnedException();
        }
    }

    private void closeIfExpired(Exam exam, ExamAttempt attempt, Instant now) {
        if (attempt.isExpiredAt(now)) {
            close(exam, attempt, now);
        }
    }

    private void close(Exam exam, ExamAttempt attempt, Instant now) {
        AttemptPaper paper = paperOf(exam, attempt);
        attempt.close(now, AttemptGrader.grade(paper.placed(), paper.questions(),
                paper.answers()));
        if (attempt.isGraded()) {
            grading.recordBestAutomatically(exam, attempt.getStudentId());
        }
    }

    AttemptPaper paperOf(Exam exam, ExamAttempt attempt) {
        List<ExamQuestion> placed = examQuestionRepository.findByExamIdOrderByPosition(
                exam.getId());
        return new AttemptPaper(exam, attempt, placed,
                questionService.byId(placed.stream().map(ExamQuestion::getQuestionId).toList()),
                answerRepository.findByAttemptId(attempt.getId()).stream()
                        .collect(Collectors.toMap(AttemptAnswer::getQuestionId,
                                Function.identity())));
    }

    Disclosure disclosureFor(CourseMember member, Exam exam, ExamAttempt attempt) {
        if (member.staff()) {
            return Disclosure.ANSWERS;
        }
        if (attempt.isInProgress()) {
            return Disclosure.PAPER;
        }
        if (!grading.isReturnedTo(exam, attempt.getStudentId())) {
            return Disclosure.HIDDEN;
        }
        return exam.isShowsAnswers() ? Disclosure.ANSWERS : Disclosure.SCORES;
    }

    ExamAttempt existing(UUID attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));
    }

    private ExamAttempt locked(UUID attemptId) {
        return attemptRepository.findForUpdate(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));
    }

    private CourseMember readable(String actorUsername, Exam exam, ExamAttempt attempt) {
        CourseMember member = examService.memberFor(actorUsername, exam);
        if (!member.staff() && !attempt.belongsTo(member.studentId())) {
            throw new AttemptOutOfReachException();
        }
        return member;
    }

    private CourseMember requireOwn(String actorUsername, Exam exam, ExamAttempt attempt) {
        CourseMember member = examService.memberFor(actorUsername, exam);
        if (member.studentId() == null || !attempt.belongsTo(member.studentId())) {
            throw new AttemptOutOfReachException();
        }
        return member;
    }

    private static UUID requireStudent(CourseMember member) {
        if (member.studentId() == null) {
            throw new OnlyStudentsTakeException();
        }
        return member.studentId();
    }

    private static AttemptSummary summaryOf(ExamAttempt attempt, Disclosure disclosure) {
        return new AttemptSummary(attempt.getId(), attempt.getStudentId(), attempt.getNumber(),
                attempt.getStatus(), attempt.getStartedAt(), attempt.getDeadline(),
                attempt.getSubmittedAt(), disclosure.showsScores() ? attempt.getScore() : null);
    }
}
