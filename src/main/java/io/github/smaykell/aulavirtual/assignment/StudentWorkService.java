package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.StudentWorkResponse;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentWorkService {

    private final AssignmentService assignmentService;
    private final SubmissionRepository submissionRepository;
    private final CourseService courseService;
    private final StudentService studentService;
    private final GradeService gradeService;

    @Transactional(readOnly = true)
    public List<StudentWorkResponse> of(String actorUsername, UUID assignmentId) {
        Assignment assignment = assignmentService.existing(assignmentId);
        CourseMember member = assignmentService.memberFor(actorUsername, assignment);
        List<UUID> studentIds = member.staff()
                ? courseService.activeStudentsOf(assignment.getCourseId())
                : List.of(member.studentId());

        Map<UUID, Submission> submissions = submissionRepository
                .findByAssignmentIdAndStudentIdIn(assignmentId, studentIds).stream()
                .collect(Collectors.toMap(Submission::getStudentId, Function.identity()));
        Map<UUID, GradeResponse> grades = gradeService.visibleTo(member, GradeSource.ASSIGNMENT,
                assignmentId, studentIds);

        return studentService.summariesOf(studentIds).values().stream()
                .sorted(StudentSummary.ALPHABETICAL)
                .map(student -> StudentWorkResponse.of(student, submissions.get(student.id()),
                        grades.get(student.id())))
                .toList();
    }
}
