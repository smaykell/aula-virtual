package io.github.smaykell.aulavirtual.modules.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.teacher.TeacherService;
import io.github.smaykell.aulavirtual.modules.teacher.exception.InactiveTeacherException;
import io.github.smaykell.aulavirtual.modules.user.UserService;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import java.util.Optional;
import java.util.Set;
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
    private static final UUID TITULAR = UUID.randomUUID();
    private static final UUID OTHER_TEACHER = UUID.randomUUID();

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserService userService;

    @Mock
    private TeacherService teacherService;

    private CourseAccess courseAccess;

    @BeforeEach
    void setUp() {
        courseAccess = new CourseAccess(courseRepository, userService, teacherService);
    }

    @Test
    void whoever_manages_teachers_reaches_any_course() {
        givenTheAdmin("ana");
        Course course = givenTheCourse(TITULAR);

        assertThat(courseAccess.readable("ana", course.getId())).isSameAs(course);
    }

    @Test
    void the_titular_teacher_reaches_its_own_course() {
        givenTheTeacher("juan", TITULAR);
        Course course = givenTheCourse(TITULAR);

        assertThat(courseAccess.readable("juan", course.getId())).isSameAs(course);
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

        assertThat(courseAccess.readable("ana", course.getId())).isSameAs(course);

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
        when(teacherService.activeProfileIdOf(ADMIN_PERSON)).thenReturn(Optional.empty());

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

        assertThat(courseAccess.teacherFilterFor("ana", null)).isNull();
        assertThat(courseAccess.teacherFilterFor("ana", TITULAR)).isEqualTo(TITULAR);
    }

    @Test
    void the_listing_of_a_teacher_is_forced_to_its_own_courses() {
        givenTheTeacher("juan", TITULAR);

        assertThat(courseAccess.teacherFilterFor("juan", null)).isEqualTo(TITULAR);
    }

    @Test
    void a_teacher_cannot_list_the_courses_of_another_teacher() {
        givenTheTeacher("juan", TITULAR);

        ApiException error = assertThrows(ApiException.class,
                () -> courseAccess.teacherFilterFor("juan", OTHER_TEACHER));

        assertThat(error.getCode()).isEqualTo("CRS_OUT_OF_REACH");
    }

    private void givenTheAdmin(String username) {
        when(userService.actor(username))
                .thenReturn(new Actor(ADMIN_PERSON, username, Set.of(Role.ADMIN)));
    }

    private void givenTheTeacher(String username, UUID teacherId) {
        when(userService.actor(username))
                .thenReturn(new Actor(TEACHER_PERSON, username, Set.of(Role.TEACHER)));
        when(teacherService.activeProfileIdOf(TEACHER_PERSON)).thenReturn(Optional.of(teacherId));
    }

    private Course givenTheCourse(UUID teacherId) {
        Course course = CourseFixtures.course(teacherId);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        return course;
    }
}
