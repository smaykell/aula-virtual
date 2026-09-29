package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.AssignmentData;
import io.github.smaykell.aulavirtual.assignment.dto.AssignmentResponse;
import io.github.smaykell.aulavirtual.assignment.exception.AssignmentHasWorkException;
import io.github.smaykell.aulavirtual.assignment.exception.AssignmentNotFoundException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.course.unit.UnitService;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.GradingSchemeService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UnitService unitService;
    private final CourseService courseService;
    private final GradeService gradeService;
    private final GradingSchemeService gradingSchemeService;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(String actorUsername, UUID unitId) {
        courseService.memberOf(actorUsername, unitService.courseOf(unitId));

        return assignmentRepository.findByUnitIdOrderByDueAt(unitId).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(String actorUsername, UUID assignmentId) {
        return AssignmentResponse.from(readable(actorUsername, assignmentId));
    }

    @Transactional
    public AssignmentResponse create(String actorUsername, UUID unitId, AssignmentData data) {
        UUID courseId = unitService.courseOf(unitId);
        courseService.requireWritable(actorUsername, courseId);
        requireCategoryOfTheCourse(courseId, data);

        return AssignmentResponse.from(
                assignmentRepository.save(Assignment.create(unitId, courseId, data)));
    }

    @Transactional
    public AssignmentResponse update(String actorUsername, UUID assignmentId,
            AssignmentData data) {

        Assignment assignment = writable(actorUsername, assignmentId);
        requireCategoryOfTheCourse(assignment.getCourseId(), data);
        assignment.update(data);
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public void delete(String actorUsername, UUID assignmentId) {
        Assignment assignment = writable(actorUsername, assignmentId);
        requireNoWork(assignment);
        assignmentRepository.delete(assignment);
    }

    Assignment writable(String actorUsername, UUID assignmentId) {
        Assignment assignment = existing(assignmentId);
        courseService.requireWritable(actorUsername, assignment.getCourseId());
        return assignment;
    }

    CourseMember memberFor(String actorUsername, Assignment assignment) {
        return courseService.memberOf(actorUsername, assignment.getCourseId());
    }

    Assignment existing(UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
    }

    private Assignment readable(String actorUsername, UUID assignmentId) {
        Assignment assignment = existing(assignmentId);
        memberFor(actorUsername, assignment);
        return assignment;
    }

    private void requireCategoryOfTheCourse(UUID courseId, AssignmentData data) {
        if (data.categoryId() != null) {
            gradingSchemeService.requireCategoryIn(courseId, data.categoryId());
        }
    }

    private void requireNoWork(Assignment assignment) {
        if (submissionRepository.existsByAssignmentId(assignment.getId())
                || gradeService.anyFor(GradeSource.ASSIGNMENT, assignment.getId())) {
            throw new AssignmentHasWorkException();
        }
    }
}
