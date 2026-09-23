package io.github.smaykell.aulavirtual.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.dto.CourseResponse;
import io.github.smaykell.aulavirtual.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.course.dto.UpdateCourseRequest;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentPolicy;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final UUID TITULAR = UUID.randomUUID();
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAccess courseAccess;

    @Mock
    private Invitations invitations;

    @Mock
    private TeacherService teacherService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, courseAccess, invitations,
                teacherService);
    }

    @Test
    void creating_a_course_resolves_the_titular_and_stamps_an_invitation_code() {
        when(courseAccess.resolveTitular("juan", null)).thenReturn(TITULAR);
        when(invitations.nextCode()).thenReturn(CourseFixtures.INVITATION_CODE);
        givenTheCourseIsStored();
        givenTheSummaryOf(TITULAR);

        CourseResponse course = courseService.create("juan",
                CourseFixtures.createRequest(null));

        assertThat(course.name()).isEqualTo("Algebra Lineal");
        assertThat(course.invitation().code()).isEqualTo(CourseFixtures.INVITATION_CODE);
        assertThat(course.invitation().url()).isEqualTo(CourseFixtures.INVITATION_URL);
        assertThat(course.status()).isEqualTo(CourseStatus.ACTIVE);
        assertThat(course.teacher().id()).isEqualTo(TITULAR);
    }

    @Test
    void a_course_that_ends_before_it_starts_is_not_stored() {
        when(courseAccess.resolveTitular("ana", TITULAR)).thenReturn(TITULAR);

        ApiException error = assertThrows(ApiException.class, () -> courseService.create("ana",
                new CreateCourseRequest("Algebra Lineal", null, TITULAR,
                        EnrollmentPolicy.ON_REQUEST, CourseFixtures.END, CourseFixtures.START)));

        assertThat(error.getCode()).isEqualTo("CRS_INVALID_DATES");
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updating_a_course_checks_the_titular_it_is_handed_to() {
        Course course = givenTheCourse();
        UUID newTitular = UUID.randomUUID();
        givenTheSummaryOf(newTitular);

        CourseResponse updated = courseService.update("ana", course.getId(),
                new UpdateCourseRequest("Algebra Lineal II", "Segundo ciclo", newTitular,
                        EnrollmentPolicy.AUTOMATIC, CourseFixtures.START,
                        LocalDate.of(2026, 8, 1)));

        verify(courseAccess).requireTitular("ana", course, newTitular);
        assertThat(updated.name()).isEqualTo("Algebra Lineal II");
        assertThat(course.acceptsEnrollmentsWithoutApproval()).isTrue();
        assertThat(course.getTeacherId()).isEqualTo(newTitular);
        assertThat(course.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void archiving_a_course_keeps_it_readable_and_activating_it_brings_it_back() {
        Course course = CourseFixtures.course(TITULAR);
        when(courseAccess.managed("ana", course.getId())).thenReturn(course);
        givenTheSummaryOf(TITULAR);

        assertThat(courseService.archive("ana", course.getId()).status())
                .isEqualTo(CourseStatus.ARCHIVED);
        assertThat(courseService.activate("ana", course.getId()).status())
                .isEqualTo(CourseStatus.ACTIVE);
    }

    @Test
    void the_listing_goes_through_the_filter_of_the_actor_and_joins_its_teacher() {
        Course course = CourseFixtures.course(TITULAR);
        when(courseAccess.listingScope("juan", null))
                .thenReturn(new CourseAccess.Scope(TITULAR, null));
        when(courseRepository.search(TITULAR, CourseStatus.ACTIVE, FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(course), FIRST_PAGE, 1));
        when(teacherService.summariesOf(List.of(TITULAR)))
                .thenReturn(Map.of(TITULAR, CourseFixtures.teacher(TITULAR)));
        when(invitations.of(CourseFixtures.INVITATION_CODE))
                .thenReturn(CourseFixtures.invitation());

        PageResponse<CourseResponse> page = courseService.list("juan", null,
                CourseStatus.ACTIVE, FIRST_PAGE);

        assertThat(page.content()).singleElement().satisfies(found ->
                assertThat(found.teacher().lastName()).isEqualTo("Perez Gomez"));
    }

    private Course givenTheCourse() {
        Course course = CourseFixtures.course(TITULAR);
        when(courseAccess.writable("ana", course.getId())).thenReturn(course);
        return course;
    }

    private void givenTheCourseIsStored() {
        when(courseRepository.save(any(Course.class))).thenAnswer(call -> {
            Course course = call.getArgument(0);
            ReflectionTestUtils.setField(course, "id", UUID.randomUUID());
            return course;
        });
    }

    private void givenTheSummaryOf(UUID teacherId) {
        when(teacherService.summaryOf(teacherId)).thenReturn(CourseFixtures.teacher(teacherId));
        when(invitations.of(CourseFixtures.INVITATION_CODE))
                .thenReturn(CourseFixtures.invitation());
    }
}
