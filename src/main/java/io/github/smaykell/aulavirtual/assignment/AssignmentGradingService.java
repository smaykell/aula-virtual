package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.GradeData;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeEntry;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentGradingService {

    private final AssignmentService assignmentService;
    private final SubmissionRepository submissionRepository;
    private final CourseService courseService;
    private final GradeService gradeService;

    @Transactional
    public GradeResponse grade(String actorUsername, UUID assignmentId, UUID studentId,
            GradeData data) {

        Assignment assignment = assignmentService.writable(actorUsername, assignmentId);
        courseService.requireActiveStudent(assignment.getCourseId(), studentId);

        GradeResponse grade = gradeService.record(actorUsername,
                entryFor(assignment, studentId, data));
        submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .ifPresent(Submission::markGraded);
        return grade;
    }

    private static GradeEntry entryFor(Assignment assignment, UUID studentId, GradeData data) {
        return new GradeEntry(GradeSource.ASSIGNMENT, assignment.getId(),
                assignment.getCourseId(), studentId, assignment.getMaxScore(), data.score(),
                data.feedback());
    }
}
