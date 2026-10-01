package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.exam.dto.AttemptResponse;
import io.github.smaykell.aulavirtual.exam.dto.AttemptSummary;
import io.github.smaykell.aulavirtual.exam.dto.ExamResultResponse;
import io.github.smaykell.aulavirtual.exam.dto.HandBackData;
import io.github.smaykell.aulavirtual.exam.dto.ScoreData;
import io.github.smaykell.aulavirtual.exam.exception.AttemptInProgressException;
import io.github.smaykell.aulavirtual.exam.exception.NotAnsweredException;
import io.github.smaykell.aulavirtual.exam.exception.PointsOutOfRangeException;
import io.github.smaykell.aulavirtual.exam.exception.QuestionNotFoundException;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamReviewService {

    private final ExamService examService;
    private final AttemptService attemptService;
    private final ExamAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final CourseService courseService;
    private final StudentService studentService;
    private final GradeService gradeService;
    private final ExamGrading grading;

    @Transactional
    public List<ExamResultResponse> results(String actorUsername, UUID examId) {
        Exam exam = examService.existing(examId);
        CourseMember member = examService.memberFor(actorUsername, exam);
        List<UUID> studentIds = member.staff()
                ? courseService.activeStudentsOf(exam.getCourseId())
                : List.of(member.studentId());

        List<ExamAttempt> attempts = attemptRepository.findByExamIdAndStudentIdInOrderByNumber(
                examId, studentIds);
        attemptService.closeExpiredAmong(exam, attempts);
        Map<UUID, List<AttemptSummary>> attemptsByStudent = attempts.stream()
                .collect(Collectors.groupingBy(ExamAttempt::getStudentId,
                        Collectors.mapping(attempt -> attemptService.summaryFor(member, exam,
                                attempt), Collectors.toList())));
        Map<UUID, GradeResponse> grades = gradeService.visibleTo(member, GradeSource.EXAM,
                examId, studentIds);

        return studentService.summariesOf(studentIds).values().stream()
                .sorted(StudentSummary.ALPHABETICAL)
                .map(student -> new ExamResultResponse(student,
                        attemptsByStudent.getOrDefault(student.id(), List.of()),
                        grades.get(student.id())))
                .toList();
    }

    @Transactional
    public AttemptResponse score(String actorUsername, UUID attemptId, UUID questionId,
            ScoreData data) {

        ExamAttempt attempt = attemptService.locked(attemptId);
        Exam exam = examService.writable(actorUsername, attempt.getExamId());
        if (attempt.isInProgress()) {
            throw new AttemptInProgressException();
        }
        requireWithinPoints(exam, questionId, data);
        answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseThrow(NotAnsweredException::new)
                .award(data.points(), data.feedback());

        AttemptPaper paper = attemptService.paperOf(exam, attempt);
        attempt.settle(AttemptGrader.grade(paper.placed(), paper.questions(), paper.answers()));
        if (attempt.isGraded()) {
            grading.recordBestBy(actorUsername, exam, attempt.getStudentId());
        }
        return paper.disclosed(Disclosure.ANSWERS);
    }

    @Transactional
    public List<GradeResponse> handBack(String actorUsername, UUID examId, HandBackData data) {
        examService.writable(actorUsername, examId);
        return gradeService.handBack(GradeSource.EXAM, examId, data.studentIds());
    }

    private void requireWithinPoints(Exam exam, UUID questionId, ScoreData data) {
        ExamQuestion placed = examQuestionRepository
                .findByExamIdAndQuestionId(exam.getId(), questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        if (data.points().compareTo(placed.getPoints()) > 0) {
            throw new PointsOutOfRangeException(placed.getPoints());
        }
    }
}
