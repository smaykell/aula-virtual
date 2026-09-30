package io.github.smaykell.aulavirtual.course.enrollment;

import io.github.smaykell.aulavirtual.course.Course;
import io.github.smaykell.aulavirtual.course.CourseRepository;
import io.github.smaykell.aulavirtual.course.enrollment.dto.CourseInvitationResponse;
import io.github.smaykell.aulavirtual.course.enrollment.dto.SelfRegistrationRequest;
import io.github.smaykell.aulavirtual.course.enrollment.dto.SelfRegistrationResponse;
import io.github.smaykell.aulavirtual.course.exception.AccountAlreadyRegisteredException;
import io.github.smaykell.aulavirtual.course.exception.CourseNotOpenException;
import io.github.smaykell.aulavirtual.course.exception.InvalidInvitationException;
import io.github.smaykell.aulavirtual.course.exception.SelfRegistrationClosedException;
import io.github.smaykell.aulavirtual.course.exception.SelfRegistrationDocumentException;
import io.github.smaykell.aulavirtual.notification.NotificationService;
import io.github.smaykell.aulavirtual.notification.NotificationType;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.exception.EmailRequiredException;
import io.github.smaykell.aulavirtual.person.exception.EmailTakenException;
import io.github.smaykell.aulavirtual.settings.SettingsService;
import io.github.smaykell.aulavirtual.settings.dto.SettingsResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.RegisteredStudent;
import io.github.smaykell.aulavirtual.student.exception.StudentAlreadyRegisteredException;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import io.github.smaykell.aulavirtual.teacher.dto.TeacherSummary;
import io.github.smaykell.aulavirtual.user.exception.UsernameTakenException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SelfEnrollmentService {

    private static final Set<DocumentType> SELF_SERVICE_DOCUMENTS =
            Set.of(DocumentType.DNI, DocumentType.FOREIGNER_CARD);

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final PersonService personService;
    private final TeacherService teacherService;
    private final SettingsService settingsService;
    private final NotificationService notificationService;
    private final EnrollmentNotices notices;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CourseInvitationResponse preview(String code) {
        Course course = openCourse(code);
        TeacherSummary teacher = teacherService.summaryOf(course.getTeacherId());
        SettingsResponse settings = settingsService.current();

        return new CourseInvitationResponse(course.getName(),
                teacher.firstName() + " " + teacher.lastName(),
                settings.studentIdentifier(), settings.selfRegistrationEnabled());
    }

    @Transactional
    public SelfRegistrationResponse register(String code, SelfRegistrationRequest request) {
        if (!settingsService.current().selfRegistrationEnabled()) {
            throw new SelfRegistrationClosedException();
        }
        Course course = openCourse(code);
        PersonData person = request.person();
        requireSelfServiceDocument(person);
        requireEmail(person);
        requireNobodyIsUsing(person);

        RegisteredStudent student = enrol(person, request.workplace());
        Enrollment enrollment = enrollmentRepository.save(Enrollment.request(course.getId(),
                student.id(), course.getEnrollmentPolicy(), clock.instant()));

        announce(course, person, student, enrollment);
        return new SelfRegistrationResponse(student.username(), enrollment.getStatus());
    }

    // Todo lo que signifique "ese dato ya esta registrado" responde el mismo codigo: dos
    // codigos distintos convierten una ruta publica en un oraculo de quien tiene cuenta aqui.
    private RegisteredStudent enrol(PersonData person, String workplace) {
        try {
            return studentService.register(person, workplace);
        } catch (EmailTakenException | UsernameTakenException
                | StudentAlreadyRegisteredException alreadyThere) {
            throw new AccountAlreadyRegisteredException();
        }
    }

    private void requireNobodyIsUsing(PersonData person) {
        boolean known = personService
                .findByDocument(person.documentType(), person.documentNumber()).isPresent()
                || personService.findByEmail(person.normalizedEmail()).isPresent();
        if (known) {
            throw new AccountAlreadyRegisteredException();
        }
    }

    private static void requireSelfServiceDocument(PersonData person) {
        if (!SELF_SERVICE_DOCUMENTS.contains(person.documentType())) {
            throw new SelfRegistrationDocumentException();
        }
    }

    private static void requireEmail(PersonData person) {
        if (person.normalizedEmail() == null) {
            throw new EmailRequiredException();
        }
    }

    private Course openCourse(String code) {
        Course course = courseRepository.findByInvitationCode(code.trim())
                .orElseThrow(InvalidInvitationException::new);
        if (course.isArchived()) {
            throw new CourseNotOpenException();
        }
        return course;
    }

    private void announce(Course course, PersonData person, RegisteredStudent student,
            Enrollment enrollment) {

        notificationService.enqueue(NotificationType.ACCOUNT_CREATED, person.normalizedEmail(),
                Map.of("firstName", person.firstName(), "username", student.username()));
        notices.joined(course, enrollment);
    }
}
