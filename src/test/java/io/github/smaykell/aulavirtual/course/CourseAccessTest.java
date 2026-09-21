package io.github.smaykell.aulavirtual.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import io.github.smaykell.aulavirtual.teacher.exception.InactiveTeacherException;
import io.github.smaykell.aulavirtual.user.UserService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAccessTest {

    private static final UUID ADMIN_PERSON = UUID.randomUUID();
    private static final UUID TEACHER_PERSON = UUID.randomUUID();
    private static final UUID STUDENT_PERSON = UUID.randomUUID();
    private static final UUID TITULAR = UUID.randomUUID();
    private static final UUID OTHER_TEACHER = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserService userService;

    @Mock
    private TeacherService teacherService;

    private CourseAccess courseAccess;

    @BeforeEach
    void setUp() {
        courseAccess = new CourseAccess(courseRepository, enrollmentRepository, userService,
                teacherService);
    }

    @Test
    void whoever_manages_teachers_reaches_any_course_as_staff() {
        givenTheAdmin("ana");
        Course course = givenTheCourse(TITULAR);

        CourseAccess.Reader reader = courseAccess.readable("ana", course.getId());

        assertThat(reader.course()).isSameAs(course);
        assertThat(reader.staff()).isTrue();
    }

    @Test
    void the_titular_teacher_reaches_its_own_course_as_staff() {
        givenTheTeacher("juan", TITULAR);
        Course course = givenTheCourse(TITULAR);

        assertThat(courseAccess.readable("juan", course.getId()).staff()).isTrue();
    }

    @Test
    void another_teacher_does_not_reach_the_course() {
        givenTheTeacher("otro", OTHER_TEACHER);
        Course course = givenTheCourse(TITULAR);

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.readable("otro", course.getId()));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
    }

    @Test
    void an_enrolled_student_reads_the_course_but_never_as_staff() {
        Course course = givenTheCourse(TITULAR);
        givenTheStudent("ana.estudiante");
        givenTheEnrollment(course, EnrollmentStatus.ACTIVE);

        CourseAccess.Reader reader = courseAccess.readable("ana.estudiante", course.getId());

        assertThat(reader.course()).isSameAs(course);
        assertThat(reader.staff()).isFalse();
    }

    @Test
    void a_student_whose_enrollment_is_not_active_does_not_reach_the_course() {
        Course course = givenTheCourse(TITULAR);
        givenTheStudent("ana.estudiante");
        givenTheEnrollment(course, EnrollmentStatus.PENDING);

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.readable("ana.estudiante", course.getId()));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
    }

    @Test
    void an_enrolled_student_still_cannot_write_the_course() {
        Course course = givenTheCourse(TITULAR);
        givenTheStudent("ana.estudiante");

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.writable("ana.estudiante", course.getId()));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
    }

    @Test
    void an_unknown_course_is_not_found() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.readable("ana", courseId));

        assertThat(error.getCode()).isEqualTo("CRS_NOT_FOUND");
    }

    @Test
    void an_archived_course_is_readable_but_not_writable() {
        givenTheAdmin("ana");
        Course course = givenTheCourse(TITULAR);
        course.archive();

        assertThat(courseAccess.readable("ana", course.getId()).course()).isSameAs(course);

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.writable("ana", course.getId()));

        assertThat(error.getCode()).isEqualTo("CRS_ARCHIVED");
    }

    @Test
    void a_teacher_that_does_not_name_a_titular_becomes_the_titular() {
        givenTheTeacher("juan", TITULAR);

        assertThat(courseAccess.resolveTitular("juan", null)).isEqualTo(TITULAR);
        verify(teacherService).requireActive(TITULAR);
    }

    @Test
    void an_admin_has_to_name_the_titular_because_it_does_not_teach() {
        givenTheAdmin("ana");

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.resolveTitular("ana", null));

        assertThat(error.getCode()).isEqualTo("CRS_TEACHER_REQUIRED");
    }

    @Test
    void a_teacher_cannot_hand_its_course_to_another_teacher() {
        givenTheTeacher("juan", TITULAR);

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.requireTitular("juan", OTHER_TEACHER));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
    }

    @Test
    void a_titular_that_is_no_longer_active_is_rejected() {
        givenTheAdmin("ana");
        doThrow(new InactiveTeacherException(TITULAR)).when(teacherService).requireActive(TITULAR);

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.requireTitular("ana", TITULAR));

        assertThat(error.getCode()).isEqualTo("TCH_INACTIVE");
    }

    @Test
    void the_listing_of_an_admin_keeps_the_filter_it_asked_for() {
        givenTheAdmin("ana");

        assertThat(courseAccess.listingScope("ana", null).teacherId()).isNull();
        assertThat(courseAccess.listingScope("ana", TITULAR).teacherId()).isEqualTo(TITULAR);
        assertThat(courseAccess.listingScope("ana", null).staff()).isTrue();
    }

    @Test
    void the_listing_of_a_teacher_is_forced_to_its_own_courses() {
        givenTheTeacher("juan", TITULAR);

        assertThat(courseAccess.listingScope("juan", null).teacherId()).isEqualTo(TITULAR);
    }

    @Test
    void a_teacher_cannot_list_the_courses_of_another_teacher() {
        givenTheTeacher("juan", TITULAR);

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.listingScope("juan", OTHER_TEACHER));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
    }

    @Test
    void the_listing_of_a_student_is_the_one_of_its_enrollments() {
        givenTheStudent("ana.estudiante");

        CourseAccess.Scope scope = courseAccess.listingScope("ana.estudiante", null);

        assertThat(scope.studentId()).isEqualTo(STUDENT);
        assertThat(scope.teacherId()).isNull();
        assertThat(scope.staff()).isFalse();
    }

    @Test
    void whoever_is_neither_teacher_nor_student_cannot_enroll() {
        givenTheAdmin("ana");

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.requireStudent("ana"));

        assertThat(error.getCode()).isEqualTo("CRS_STUDENT_REQUIRED");
    }

    private void givenTheAdmin(String username) {
        when(userService.actor(username)).thenReturn(new Actor(ADMIN_PERSON, username,
                Map.of(Role.ADMIN, UUID.randomUUID())));
    }

    private void givenTheTeacher(String username, UUID teacherId) {
        when(userService.actor(username)).thenReturn(new Actor(TEACHER_PERSON, username,
                Map.of(Role.TEACHER, teacherId)));
    }

    private void givenTheStudent(String username) {
        when(userService.actor(username)).thenReturn(new Actor(STUDENT_PERSON, username,
                Map.of(Role.STUDENT, STUDENT)));
    }

    private void givenTheEnrollment(Course course, EnrollmentStatus status) {
        when(enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(course.getId(), STUDENT,
                EnrollmentStatus.ACTIVE)).thenReturn(status == EnrollmentStatus.ACTIVE);
    }

    private Course givenTheCourse(UUID teacherId) {
        Course course = CourseFixtures.course(teacherId);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        return course;
    }
}
