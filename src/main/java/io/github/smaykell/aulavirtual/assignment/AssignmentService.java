package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.dto.AssignmentData;
import io.github.smaykell.aulavirtual.assignment.dto.AssignmentResponse;
import io.github.smaykell.aulavirtual.assignment.exception.AssignmentNotFoundException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.course.unit.UnitService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final UnitService unitService;
    private final CourseService courseService;

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
        courseService.requireWritable(actorUsername, unitService.courseOf(unitId));

        return AssignmentResponse.from(
                assignmentRepository.save(Assignment.create(unitId, data)));
    }

    @Transactional
    public AssignmentResponse update(String actorUsername, UUID assignmentId,
            AssignmentData data) {

        Assignment assignment = writable(actorUsername, assignmentId);
        assignment.update(data);
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public void delete(String actorUsername, UUID assignmentId) {
        assignmentRepository.delete(writable(actorUsername, assignmentId));
    }

    Assignment writable(String actorUsername, UUID assignmentId) {
        Assignment assignment = existing(assignmentId);
        courseService.requireWritable(actorUsername, courseOf(assignment));
        return assignment;
    }

    CourseMember memberFor(String actorUsername, Assignment assignment) {
        return courseService.memberOf(actorUsername, courseOf(assignment));
    }

    UUID courseOf(Assignment assignment) {
        return unitService.courseOf(assignment.getUnitId());
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
}
