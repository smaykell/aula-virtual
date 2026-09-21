package io.github.smaykell.aulavirtual.course.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.CourseFixtures;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialData;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");
    private static final UUID COURSE = UUID.randomUUID();

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private UnitService unitService;

    private MaterialService materialService;

    private Unit unit;

    @BeforeEach
    void setUp() {
        materialService = new MaterialService(materialRepository, unitService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        unit = CourseFixtures.unit(COURSE, "Semana 1", 1);
    }

    @Test
    void a_material_without_a_publication_date_is_published_now() {
        givenTheUnitIsWritable();
        givenTheMaterialIsStored();

        MaterialResponse material = materialService.create("juan", unit.getId(),
                CourseFixtures.file("Tema 1", MaterialType.PDF));

        assertThat(material.publishedAt()).isEqualTo(NOW);
        assertThat(material.storageKey()).isEqualTo("courses/algebra/tema-1.pdf");
        assertThat(material.externalUrl()).isNull();
    }

    @Test
    void a_material_with_a_publication_date_keeps_it_for_the_future() {
        Instant tomorrow = NOW.plusSeconds(86400);
        givenTheUnitIsWritable();
        givenTheMaterialIsStored();

        MaterialResponse material = materialService.create("juan", unit.getId(),
                new MaterialData("Tema 2", MaterialType.PDF, "courses/algebra/tema-2.pdf", null,
                        tomorrow, false));

        assertThat(material.publishedAt()).isEqualTo(tomorrow);
        assertThat(material.visible()).isFalse();
    }

    @Test
    void a_link_keeps_its_url_and_no_storage_key() {
        givenTheUnitIsWritable();
        givenTheMaterialIsStored();

        MaterialResponse material = materialService.create("juan", unit.getId(),
                CourseFixtures.link("Clase grabada"));

        assertThat(material.externalUrl()).isEqualTo("https://example.org/clase");
        assertThat(material.storageKey()).isNull();
    }

    @Test
    void a_link_without_url_is_rejected() {
        givenTheUnitIsWritable();

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.create("juan", unit.getId(),
                        new MaterialData("Clase", MaterialType.LINK, null, "  ", null, true)));

        assertThat(error.getCode()).isEqualTo("CRS_MATERIAL_NEEDS_URL");
        verify(materialRepository, never()).save(any(Material.class));
    }

    @Test
    void a_file_without_its_key_is_rejected() {
        givenTheUnitIsWritable();

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.create("juan", unit.getId(),
                        new MaterialData("Tema 1", MaterialType.PDF, null, null, null, true)));

        assertThat(error.getMessage()).contains("PDF");
        assertThat(error.getCode()).isEqualTo("CRS_MATERIAL_NEEDS_FILE");
    }

    @Test
    void updating_a_material_turns_a_file_into_a_link() {
        Material material = CourseFixtures.material(unit.getId(),
                CourseFixtures.file("Tema 1", MaterialType.PDF), NOW);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(unitService.writable("juan", unit.getId())).thenReturn(unit);

        MaterialResponse updated = materialService.update("juan", material.getId(),
                CourseFixtures.link("Clase grabada"));

        assertThat(updated.type()).isEqualTo(MaterialType.LINK);
        assertThat(updated.storageKey()).isNull();
        assertThat(material.getExternalUrl()).isEqualTo("https://example.org/clase");
    }

    @Test
    void an_unknown_material_is_not_found() {
        UUID materialId = UUID.randomUUID();
        when(materialRepository.findById(materialId)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.delete("juan", materialId));

        assertThat(error.getCode()).isEqualTo("CRS_MATERIAL_NOT_FOUND");
    }

    private void givenTheUnitIsWritable() {
        when(unitService.writable("juan", unit.getId())).thenReturn(unit);
    }

    private void givenTheMaterialIsStored() {
        when(materialRepository.save(any(Material.class))).thenAnswer(call -> {
            Material material = call.getArgument(0);
            ReflectionTestUtils.setField(material, "id", UUID.randomUUID());
            return material;
        });
    }
}
