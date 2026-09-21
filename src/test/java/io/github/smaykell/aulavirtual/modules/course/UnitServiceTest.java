package io.github.smaykell.aulavirtual.modules.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.modules.course.dto.ReorderUnitsRequest;
import io.github.smaykell.aulavirtual.modules.course.dto.UnitData;
import io.github.smaykell.aulavirtual.modules.course.dto.UnitResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    private static final UUID TITULAR = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private CourseAccess courseAccess;

    private UnitService unitService;

    private Course course;

    @BeforeEach
    void setUp() {
        unitService = new UnitService(unitRepository, materialRepository, courseAccess,
                Clock.fixed(NOW, ZoneOffset.UTC));
        course = CourseFixtures.course(TITULAR);
    }

    @Test
    void the_first_unit_of_a_course_opens_the_order() {
        givenTheCourseIsWritable();
        when(unitRepository.findLastPosition(course.getId())).thenReturn(Optional.empty());
        givenTheUnitIsStored();

        assertThat(unitService.create("juan", course.getId(), new UnitData("Semana 1")).position())
                .isEqualTo(1);
    }

    @Test
    void a_new_unit_goes_after_the_last_one() {
        givenTheCourseIsWritable();
        when(unitRepository.findLastPosition(course.getId())).thenReturn(Optional.of(4));
        givenTheUnitIsStored();

        assertThat(unitService.create("juan", course.getId(), new UnitData("Semana 5")).position())
                .isEqualTo(5);
    }

    @Test
    void the_listing_hands_each_unit_its_material() {
        Unit first = CourseFixtures.unit(course.getId(), "Semana 1", 1);
        Unit second = CourseFixtures.unit(course.getId(), "Semana 2", 2);
        when(courseAccess.readable("juan", course.getId()))
                .thenReturn(new CourseAccess.Reader(course, true));
        when(unitRepository.findByCourseIdOrderByPosition(course.getId()))
                .thenReturn(List.of(first, second));
        when(materialRepository.findByUnitIdInOrderByPublishedAt(
                List.of(first.getId(), second.getId())))
                .thenReturn(List.of(CourseFixtures.material(first.getId(),
                        CourseFixtures.file("Tema 1", MaterialType.PDF), NOW)));

        List<UnitResponse> units = unitService.list("juan", course.getId());

        assertThat(units.get(0).materials()).singleElement()
                .satisfies(material -> assertThat(material.title()).isEqualTo("Tema 1"));
        assertThat(units.get(1).materials()).isEmpty();
    }

    @Test
    void a_student_only_sees_the_material_already_published() {
        Unit unit = CourseFixtures.unit(course.getId(), "Semana 1", 1);
        when(courseAccess.readable("ana.estudiante", course.getId()))
                .thenReturn(new CourseAccess.Reader(course, false));
        when(unitRepository.findByCourseIdOrderByPosition(course.getId()))
                .thenReturn(List.of(unit));
        when(materialRepository.findByUnitIdInOrderByPublishedAt(List.of(unit.getId())))
                .thenReturn(List.of(
                        CourseFixtures.material(unit.getId(),
                                CourseFixtures.file("Publicado", MaterialType.PDF),
                                NOW.minusSeconds(60)),
                        CourseFixtures.material(unit.getId(),
                                CourseFixtures.file("Programado", MaterialType.PDF),
                                NOW.plusSeconds(60)),
                        CourseFixtures.material(unit.getId(),
                                CourseFixtures.hidden("Borrador", MaterialType.PDF),
                                NOW.minusSeconds(60))));

        List<UnitResponse> units = unitService.list("ana.estudiante", course.getId());

        assertThat(units).singleElement().satisfies(found ->
                assertThat(found.materials()).singleElement().satisfies(material ->
                        assertThat(material.title()).isEqualTo("Publicado")));
    }

    @Test
    void the_teacher_sees_the_material_that_the_student_still_cannot_see() {
        Unit unit = CourseFixtures.unit(course.getId(), "Semana 1", 1);
        when(courseAccess.readable("juan", course.getId()))
                .thenReturn(new CourseAccess.Reader(course, true));
        when(unitRepository.findByCourseIdOrderByPosition(course.getId()))
                .thenReturn(List.of(unit));
        when(materialRepository.findByUnitIdInOrderByPublishedAt(List.of(unit.getId())))
                .thenReturn(List.of(
                        CourseFixtures.material(unit.getId(),
                                CourseFixtures.hidden("Borrador", MaterialType.PDF),
                                NOW.plusSeconds(60))));

        assertThat(unitService.list("juan", course.getId()).get(0).materials()).hasSize(1);
    }

    @Test
    void reordering_rewrites_the_positions_in_the_order_it_is_given() {
        Unit first = CourseFixtures.unit(course.getId(), "Semana 1", 1);
        Unit second = CourseFixtures.unit(course.getId(), "Semana 2", 2);
        givenTheUnitsOfTheCourse(first, second);

        List<UnitResponse> units = unitService.reorder("juan", course.getId(),
                new ReorderUnitsRequest(List.of(second.getId(), first.getId())));

        assertThat(second.getPosition()).isEqualTo(1);
        assertThat(first.getPosition()).isEqualTo(2);
        assertThat(units.get(0).title()).isEqualTo("Semana 2");
    }

    @Test
    void an_order_that_leaves_out_a_unit_is_rejected() {
        Unit first = CourseFixtures.unit(course.getId(), "Semana 1", 1);
        Unit second = CourseFixtures.unit(course.getId(), "Semana 2", 2);
        givenTheUnitsOfTheCourse(first, second);

        ApiException error = assertThrows(ApiException.class,
                () -> unitService.reorder("juan", course.getId(),
                        new ReorderUnitsRequest(List.of(first.getId()))));

        assertThat(error.getCode()).isEqualTo("CRS_INVALID_UNIT_ORDER");
        assertThat(first.getPosition()).isEqualTo(1);
    }

    @Test
    void deleting_a_unit_takes_its_material_with_it() {
        Unit unit = CourseFixtures.unit(course.getId(), "Semana 1", 1);
        when(unitRepository.findById(unit.getId())).thenReturn(Optional.of(unit));
        when(courseAccess.writable("juan", course.getId())).thenReturn(course);

        unitService.delete("juan", unit.getId());

        verify(materialRepository).deleteByUnitId(unit.getId());
        verify(unitRepository).delete(unit);
    }

    @Test
    void an_unknown_unit_is_not_found() {
        UUID unitId = UUID.randomUUID();
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> unitService.update("juan", unitId, new UnitData("Semana 1")));

        assertThat(error.getCode()).isEqualTo("CRS_UNIT_NOT_FOUND");
        verify(courseAccess, never()).writable(any(), any());
    }

    private void givenTheCourseIsWritable() {
        when(courseAccess.writable("juan", course.getId())).thenReturn(course);
    }

    private void givenTheUnitsOfTheCourse(Unit... units) {
        givenTheCourseIsWritable();
        when(unitRepository.findByCourseIdOrderByPosition(course.getId()))
                .thenReturn(List.of(units));
    }

    private void givenTheUnitIsStored() {
        when(unitRepository.save(any(Unit.class))).thenAnswer(call -> {
            Unit unit = call.getArgument(0);
            ReflectionTestUtils.setField(unit, "id", UUID.randomUUID());
            return unit;
        });
    }
}
