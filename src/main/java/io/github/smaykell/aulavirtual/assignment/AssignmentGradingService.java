package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.GradeData;
import io.github.smaykell.aulavirtual.assignment.dto.HandBackRequest;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeEntry;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import java.util.List;
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

        return gradeService.record(actorUsername, entryFor(assignment, studentId, data));
    }

    @Transactional
    public List<GradeResponse> handBack(String actorUsername, UUID assignmentId,
            HandBackRequest request) {

        assignmentService.writable(actorUsername, assignmentId);
        List<GradeResponse> returned = gradeService.handBack(GradeSource.ASSIGNMENT,
                assignmentId, request.studentIds());

        List<UUID> studentsReturned = returned.stream().map(GradeResponse::studentId).toList();
        submissionRepository.findByAssignmentIdAndStudentIdIn(assignmentId, studentsReturned)
                .forEach(Submission::markGraded);
        return returned;
    }

    private static GradeEntry entryFor(Assignment assignment, UUID studentId, GradeData data) {
        return new GradeEntry(GradeSource.ASSIGNMENT, assignment.getId(),
                assignment.getCourseId(), studentId, assignment.getMaxScore(), data.score(),
                data.feedback());
    }
}
