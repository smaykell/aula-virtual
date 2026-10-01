package io.github.smaykell.aulavirtual.assignment;

import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentFiles;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentOwner;
import io.github.smaykell.aulavirtual.assignment.attachment.AttachmentService;
import io.github.smaykell.aulavirtual.assignment.dto.AssignmentBacklog;
import io.github.smaykell.aulavirtual.assignment.dto.AssignmentData;
import io.github.smaykell.aulavirtual.assignment.dto.AssignmentResponse;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentResponse;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentUploadRequest;
import io.github.smaykell.aulavirtual.assignment.dto.AttachmentUploadResponse;
import io.github.smaykell.aulavirtual.assignment.dto.UpcomingAssignment;
import io.github.smaykell.aulavirtual.assignment.exception.AssignmentHasWorkException;
import io.github.smaykell.aulavirtual.assignment.exception.AssignmentNotFoundException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.gradebook.GradeService;
import io.github.smaykell.aulavirtual.gradebook.GradeSource;
import io.github.smaykell.aulavirtual.gradebook.GradingSchemeService;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseService courseService;
    private final GradeService gradeService;
    private final GradingSchemeService gradingSchemeService;
    private final AttachmentService attachmentService;
    private final AssignmentNotices notices;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(String actorUsername, UUID unitId) {
        courseService.memberOf(actorUsername, courseService.courseOf(unitId));

        List<Assignment> assignments = assignmentRepository.findByUnitIdOrderByDueAt(unitId);
        Map<UUID, List<AttachmentResponse>> attachments = attachmentService.ofAssignments(
                assignments.stream().map(Assignment::getId).toList());
        return assignments.stream()
                .map(assignment -> AssignmentResponse.from(assignment,
                        attachments.getOrDefault(assignment.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(String actorUsername, UUID assignmentId) {
        Assignment assignment = readable(actorUsername, assignmentId);
        return AssignmentResponse.from(assignment, attachmentService.of(ownerOf(assignment)));
    }

    @Transactional(readOnly = true)
    public AttachmentUploadResponse prepareUpload(String actorUsername, UUID unitId,
            AttachmentUploadRequest request) {

        UUID courseId = courseService.courseOf(unitId);
        courseService.requireWritable(actorUsername, courseId);
        return attachmentService.prepareUpload(AttachmentFiles.assignmentPrefix(courseId),
                request);
    }

    @Transactional
    public AssignmentResponse create(String actorUsername, UUID unitId, AssignmentData data) {
        UUID courseId = courseService.courseOf(unitId);
        courseService.requireWritable(actorUsername, courseId);
        requireCategoryOfTheCourse(courseId, data);

        Assignment assignment = assignmentRepository.save(
                Assignment.create(unitId, courseId, data));
        notices.published(assignment);
        return AssignmentResponse.from(assignment, attachmentsFor(assignment, data));
    }

    @Transactional
    public AssignmentResponse update(String actorUsername, UUID assignmentId,
            AssignmentData data) {

        Assignment assignment = writable(actorUsername, assignmentId);
        requireCategoryOfTheCourse(assignment.getCourseId(), data);
        assignment.update(data);
        return AssignmentResponse.from(assignment, attachmentsFor(assignment, data));
    }

    @Transactional
    public void delete(String actorUsername, UUID assignmentId) {
        Assignment assignment = writable(actorUsername, assignmentId);
        requireNoWork(assignment);
        attachmentService.deleteAll(ownerOf(assignment));
        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<UpcomingAssignment> pendingFor(UUID studentId, Collection<UUID> courseIds,
            Instant now, Instant lateSince, int limit) {

        if (courseIds.isEmpty()) {
            return List.of();
        }
        return assignmentRepository
                .findPendingFor(studentId, courseIds, now, lateSince, Limit.of(limit))
                .stream()
                .map(UpcomingAssignment::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentBacklog> backlogIn(Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return submissionRepository.findBacklogIn(courseIds, SubmissionStatus.GRADED);
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

    private List<AttachmentResponse> attachmentsFor(Assignment assignment, AssignmentData data) {
        return attachmentService.replace(ownerOf(assignment),
                AttachmentFiles.assignmentPrefix(assignment.getCourseId()),
                data.attachmentsOrNone());
    }

    private static AttachmentOwner ownerOf(Assignment assignment) {
        return AttachmentOwner.ofAssignment(assignment.getId());
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
