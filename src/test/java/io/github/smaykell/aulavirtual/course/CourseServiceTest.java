package io.github.smaykell.aulavirtual.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.announcement.AnnouncementRepository;
import io.github.smaykell.aulavirtual.course.dto.CourseResponse;
import io.github.smaykell.aulavirtual.course.dto.CreateCourseRequest;
import io.github.smaykell.aulavirtual.course.dto.UpdateCourseRequest;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentPolicy;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentRepository;
import io.github.smaykell.aulavirtual.course.enrollment.EnrollmentStatus;
import io.github.smaykell.aulavirtual.course.unit.UnitService;
import io.github.smaykell.aulavirtual.teacher.TeacherService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private UnitService unitService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, courseAccess, invitations,
                teacherService, enrollmentRepository, announcementRepository, unitService);
    }

    @Test
    void the_students_of_a_course_are_those_with_an_active_enrollment() {
        UUID courseId = UUID.randomUUID();
        List<UUID> active = List.of(UUID.randomUUID());
        when(enrollmentRepository.findStudentIdsByCourseIdAndStatus(courseId,
                EnrollmentStatus.ACTIVE)).thenReturn(active);

        assertThat(courseService.activeStudentsOf(courseId)).isEqualTo(active);
    }

    @Test
    void a_student_without_an_active_enrollment_is_not_part_of_the_course() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(courseId, studentId,
                EnrollmentStatus.ACTIVE)).thenReturn(false);

        ApiException error = assertThrows(ApiException.class,
                () -> courseService.requireActiveStudent(courseId, studentId));

        assertThat(error.getCode()).isEqualTo("CRS_STUDENT_NOT_ENROLLED");
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
        when(courseRepository.search(TITULAR, CourseStatus.ACTIVE, "%", FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(course), FIRST_PAGE, 1));
        when(teacherService.summariesOf(List.of(TITULAR)))
                .thenReturn(Map.of(TITULAR, CourseFixtures.teacher(TITULAR)));
        when(invitations.of(CourseFixtures.INVITATION_CODE))
                .thenReturn(CourseFixtures.invitation());

        PageResponse<CourseResponse> page = courseService.list("juan", null,
                CourseStatus.ACTIVE, null, FIRST_PAGE);

        assertThat(page.content()).singleElement().satisfies(found -> {
            assertThat(found.teacher().lastName()).isEqualTo("Perez Gomez");
            assertThat(found.staff()).isTrue();
        });
    }

    @Test
    void a_staff_listing_shows_as_a_student_the_courses_the_actor_attends() {
        Course taught = CourseFixtures.course(TITULAR);
        Course attended = CourseFixtures.course(TITULAR);
        when(courseAccess.listingScope("ana", null))
                .thenReturn(new CourseAccess.Scope(null, null));
        when(courseRepository.search(null, null, "%", FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(taught, attended), FIRST_PAGE, 2));
        when(teacherService.summariesOf(List.of(TITULAR, TITULAR)))
                .thenReturn(Map.of(TITULAR, CourseFixtures.teacher(TITULAR)));
        when(courseAccess.attendedAmong("ana", List.of(taught.getId(), attended.getId())))
                .thenReturn(Set.of(attended.getId()));
        when(invitations.of(CourseFixtures.INVITATION_CODE))
                .thenReturn(CourseFixtures.invitation());

        List<CourseResponse> content = courseService.list("ana", null, null, null, FIRST_PAGE)
                .content();

        assertThat(content.get(0).staff()).isTrue();
        assertThat(content.get(0).invitation()).isNotNull();
        assertThat(content.get(1).staff()).isFalse();
        assertThat(content.get(1).invitation()).isNull();
    }

    @Test
    void whoever_attends_a_course_reads_it_as_a_student_and_without_invitation() {
        Course course = CourseFixtures.course(TITULAR);
        when(courseAccess.readable("ana", course.getId()))
                .thenReturn(new CourseAccess.Reader(course, false, UUID.randomUUID()));
        when(teacherService.summaryOf(TITULAR)).thenReturn(CourseFixtures.teacher(TITULAR));

        CourseResponse found = courseService.get("ana", course.getId());

        assertThat(found.staff()).isFalse();
        assertThat(found.invitation()).isNull();
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
