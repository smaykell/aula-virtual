package io.github.smaykell.aulavirtual.course.announcement;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.course.CourseAccess;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementData;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementResponse;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentRepository;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentStatus;
import io.github.smaykell.aulavirtual.course.exception.AnnouncementNotFoundException;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentContact;
import io.github.smaykell.aulavirtual.user.UserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseAccess courseAccess;
    private final UserService userService;
    private final PersonService personService;
    private final StudentService studentService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> list(String actorUsername, UUID courseId,
            Pageable pageable) {

        Course course = courseAccess.readable(actorUsername, courseId).course();
        Page<Announcement> announcements =
                announcementRepository.findByCourseId(course.getId(), pageable);
        Map<UUID, PersonResponse> authors = personService.byIds(announcements.getContent()
                .stream().map(Announcement::getAuthorPersonId).toList());

        return PageResponse.of(announcements, announcement -> AnnouncementResponse.from(
                announcement, nameOf(authors.get(announcement.getAuthorPersonId()))));
    }

    @Transactional
    public AnnouncementResponse publish(String actorUsername, UUID courseId,
            AnnouncementData data) {

        Course course = courseAccess.writable(actorUsername, courseId);
        UUID author = userService.actor(actorUsername).personId();
        Announcement announcement = announcementRepository.save(
                Announcement.publish(course.getId(), author, data));
        tellTheStudentsOf(course, announcement);
        return responseFor(announcement);
    }

    @Transactional
    public AnnouncementResponse update(String actorUsername, UUID announcementId,
            AnnouncementData data) {

        Announcement announcement = writable(actorUsername, announcementId);
        announcement.update(data);
        return responseFor(announcement);
    }

    @Transactional
    public void delete(String actorUsername, UUID announcementId) {
        announcementRepository.delete(writable(actorUsername, announcementId));
    }

    private Announcement writable(String actorUsername, UUID announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new AnnouncementNotFoundException(announcementId));
        courseAccess.writable(actorUsername, announcement.getCourseId());
        return announcement;
    }

    private void tellTheStudentsOf(Course course, Announcement announcement) {
        List<UUID> students = enrollmentRepository.findStudentIdsByCourseIdAndStatus(
                course.getId(), EnrollmentStatus.ACTIVE);
        if (students.isEmpty()) {
            return;
        }
        for (StudentContact student : studentService.contactsOf(students)) {
            notificationService.enqueue(NotificationType.ANNOUNCEMENT_PUBLISHED, student.email(),
                    Map.of("firstName", student.firstName(), "courseName", course.getName(),
                            "title", announcement.getTitle(), "body", announcement.getBody()));
        }
    }

    private AnnouncementResponse responseFor(Announcement announcement) {
        return AnnouncementResponse.from(announcement,
                nameOf(personService.get(announcement.getAuthorPersonId())));
    }

    private static String nameOf(PersonResponse person) {
        return person.firstName() + " " + person.lastName();
    }
}
