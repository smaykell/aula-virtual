package io.github.smaykell.aulavirtual.course.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.common.storage.FileCleanup;
import io.github.smaykell.aulavirtual.common.storage.FileStorage;
import io.github.smaykell.aulavirtual.common.storage.PresignedDownload;
import io.github.smaykell.aulavirtual.common.storage.PresignedUpload;
import io.github.smaykell.aulavirtual.common.storage.StoredObject;
import io.github.smaykell.aulavirtual.course.CourseFixtures;
import io.github.smaykell.aulavirtual.course.exception.MaterialNotFoundException;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialData;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialDownloadResponse;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialResponse;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialUploadRequest;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialUploadResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ContentDisposition;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");
    private static final UUID COURSE = UUID.randomUUID();
    private static final String KEY =
            "courses/" + COURSE + "/materials/" + UUID.randomUUID() + "/tema-1.pdf";
    private static final long ONE_MEGABYTE = 1024L * 1024L;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private UnitService unitService;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private FileCleanup fileCleanup;

    private MaterialService materialService;

    private Unit unit;

    @BeforeEach
    void setUp() {
        materialService = new MaterialService(materialRepository, unitService, fileStorage,
                fileCleanup, Clock.fixed(NOW, ZoneOffset.UTC));
        unit = CourseFixtures.unit(COURSE, "Semana 1", 1);
    }

    @Test
    void an_upload_is_signed_under_the_course_with_a_safe_file_name() {
        givenTheUnitIsWritable();
        when(fileStorage.presignUpload(anyString(), eq("application/pdf"), eq(ONE_MEGABYTE)))
                .thenReturn(new PresignedUpload("https://s3/upload", Map.of(), NOW));

        MaterialUploadResponse upload = materialService.prepareUpload("juan", unit.getId(),
                new MaterialUploadRequest(MaterialType.PDF, "Tema 1: Álgebra.PDF",
                        "application/pdf", ONE_MEGABYTE));

        assertThat(upload.storageKey())
                .startsWith("courses/" + COURSE + "/materials/")
                .endsWith("/Tema-1-Algebra.pdf");
        assertThat(upload.uploadUrl()).isEqualTo("https://s3/upload");
        assertThat(upload.method()).isEqualTo("PUT");
    }

    @Test
    void an_upload_over_the_limit_of_its_type_is_not_signed() {
        givenTheUnitIsWritable();

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.prepareUpload("juan", unit.getId(),
                        new MaterialUploadRequest(MaterialType.PDF, "tema.pdf",
                                "application/pdf", 51 * ONE_MEGABYTE)));

        assertThat(error.getCode()).isEqualTo("CRS_FILE_TOO_LARGE");
        assertThat(error.getMessage()).contains("50 MB");
        verifyNoInteractions(fileStorage);
    }

    @Test
    void an_upload_of_a_type_the_material_does_not_accept_is_not_signed() {
        givenTheUnitIsWritable();

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.prepareUpload("juan", unit.getId(),
                        new MaterialUploadRequest(MaterialType.PDF, "tema.html", "text/html",
                                ONE_MEGABYTE)));

        assertThat(error.getCode()).isEqualTo("CRS_FILE_TYPE_NOT_ALLOWED");
        verifyNoInteractions(fileStorage);
    }

    @Test
    void a_link_has_nothing_to_upload() {
        givenTheUnitIsWritable();

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.prepareUpload("juan", unit.getId(),
                        new MaterialUploadRequest(MaterialType.LINK, "clase.pdf",
                                "application/pdf", ONE_MEGABYTE)));

        assertThat(error.getCode()).isEqualTo("CRS_FILE_TYPE_NOT_ALLOWED");
    }

    @Test
    void a_material_without_a_publication_date_is_published_now() {
        givenTheUnitIsWritable();
        givenTheFileWasUploaded("application/pdf", ONE_MEGABYTE);
        givenTheMaterialIsStored();

        MaterialResponse material = materialService.create("juan", unit.getId(), pdf());

        assertThat(material.publishedAt()).isEqualTo(NOW);
        assertThat(material.storageKey()).isEqualTo(KEY);
        assertThat(material.externalUrl()).isNull();
    }

    @Test
    void a_material_with_a_publication_date_keeps_it_for_the_future() {
        Instant tomorrow = NOW.plusSeconds(86400);
        givenTheUnitIsWritable();
        givenTheFileWasUploaded("application/pdf", ONE_MEGABYTE);
        givenTheMaterialIsStored();

        MaterialResponse material = materialService.create("juan", unit.getId(),
                new MaterialData("Tema 2", MaterialType.PDF, KEY, null, tomorrow, false));

        assertThat(material.publishedAt()).isEqualTo(tomorrow);
        assertThat(material.visible()).isFalse();
    }

    @Test
    void creating_a_material_claims_its_uploaded_file() {
        givenTheUnitIsWritable();
        givenTheFileWasUploaded("application/pdf; charset=binary", ONE_MEGABYTE);
        givenTheMaterialIsStored();

        materialService.create("juan", unit.getId(), pdf());

        verify(fileStorage).claim(KEY);
    }

    @Test
    void a_file_uploaded_for_another_course_is_rejected() {
        givenTheUnitIsWritable();
        String foreignKey = "courses/" + UUID.randomUUID() + "/materials/x/tema-1.pdf";

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.create("juan", unit.getId(),
                        new MaterialData("Tema 1", MaterialType.PDF, foreignKey, null, null,
                                true)));

        assertThat(error.getCode()).isEqualTo("CRS_FOREIGN_FILE");
        verifyNoInteractions(fileStorage);
    }

    @Test
    void a_file_that_another_material_already_uses_is_rejected() {
        givenTheUnitIsWritable();
        when(materialRepository.existsByStorageKey(KEY)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.create("juan", unit.getId(), pdf()));

        assertThat(error.getCode()).isEqualTo("CRS_FOREIGN_FILE");
        verifyNoInteractions(fileStorage);
    }

    @Test
    void a_file_that_never_reached_the_storage_is_rejected() {
        givenTheUnitIsWritable();
        when(fileStorage.describe(KEY)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.create("juan", unit.getId(), pdf()));

        assertThat(error.getCode()).isEqualTo("CRS_FILE_NOT_UPLOADED");
        verify(materialRepository, never()).save(any(Material.class));
    }

    @Test
    void a_stored_file_of_another_type_is_rejected_and_not_claimed() {
        givenTheUnitIsWritable();
        givenTheFileWasUploaded("text/html", ONE_MEGABYTE);

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.create("juan", unit.getId(), pdf()));

        assertThat(error.getCode()).isEqualTo("CRS_FILE_TYPE_NOT_ALLOWED");
        verify(fileStorage, never()).claim(anyString());
    }

    @Test
    void a_stored_file_over_the_limit_is_rejected_and_not_claimed() {
        givenTheUnitIsWritable();
        givenTheFileWasUploaded("application/pdf", 60 * ONE_MEGABYTE);

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.create("juan", unit.getId(), pdf()));

        assertThat(error.getCode()).isEqualTo("CRS_FILE_TOO_LARGE");
        verify(fileStorage, never()).claim(anyString());
    }

    @Test
    void a_link_keeps_its_url_and_no_storage_key() {
        givenTheUnitIsWritable();
        givenTheMaterialIsStored();

        MaterialResponse material = materialService.create("juan", unit.getId(),
                CourseFixtures.link("Clase grabada"));

        assertThat(material.externalUrl()).isEqualTo("https://example.org/clase");
        assertThat(material.storageKey()).isNull();
        verifyNoInteractions(fileStorage);
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
    void updating_a_material_that_keeps_its_file_does_not_touch_the_storage() {
        Material material = givenAnExistingPdf();

        MaterialResponse updated = materialService.update("juan", material.getId(),
                new MaterialData("Tema 1 corregido", MaterialType.PDF, KEY, null, null, true));

        assertThat(updated.title()).isEqualTo("Tema 1 corregido");
        verifyNoInteractions(fileStorage, fileCleanup);
    }

    @Test
    void replacing_the_file_of_a_material_releases_the_previous_one() {
        Material material = givenAnExistingPdf();
        String newKey = "courses/" + COURSE + "/materials/" + UUID.randomUUID() + "/tema-1b.pdf";
        when(fileStorage.describe(newKey))
                .thenReturn(Optional.of(new StoredObject(ONE_MEGABYTE, "application/pdf")));

        materialService.update("juan", material.getId(),
                new MaterialData("Tema 1", MaterialType.PDF, newKey, null, null, true));

        verify(fileStorage).claim(newKey);
        verify(fileCleanup).deleteAfterCommit(KEY);
    }

    @Test
    void a_file_that_turns_into_a_link_is_released() {
        Material material = givenAnExistingPdf();

        materialService.update("juan", material.getId(), CourseFixtures.link("Clase grabada"));

        verify(fileCleanup).deleteAfterCommit(KEY);
    }

    @Test
    void deleting_a_material_releases_its_file() {
        Material material = givenAnExistingPdf();

        materialService.delete("juan", material.getId());

        verify(materialRepository).delete(material);
        verify(fileCleanup).deleteAfterCommit(KEY);
    }

    @Test
    void deleting_a_link_has_no_file_to_release() {
        Material material = CourseFixtures.material(unit.getId(),
                CourseFixtures.link("Clase grabada"), NOW);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        givenTheUnitIsWritable();

        materialService.delete("juan", material.getId());

        verifyNoInteractions(fileCleanup);
    }

    @Test
    void a_pdf_is_downloaded_to_open_in_the_browser() {
        Material material = givenAVisible(pdf());
        ArgumentCaptor<ContentDisposition> disposition =
                ArgumentCaptor.forClass(ContentDisposition.class);
        when(fileStorage.presignDownload(eq(KEY), disposition.capture()))
                .thenReturn(new PresignedDownload("https://s3/download", NOW));

        MaterialDownloadResponse download = materialService.download("ana", material.getId());

        assertThat(download.url()).isEqualTo("https://s3/download");
        assertThat(disposition.getValue().isInline()).isTrue();
        assertThat(disposition.getValue().getFilename()).isEqualTo("tema-1.pdf");
    }

    @Test
    void a_document_is_downloaded_as_an_attachment() {
        Material material = givenAVisible(
                new MaterialData("Guía", MaterialType.DOC, KEY, null, null, true));
        ArgumentCaptor<ContentDisposition> disposition =
                ArgumentCaptor.forClass(ContentDisposition.class);
        when(fileStorage.presignDownload(eq(KEY), disposition.capture()))
                .thenReturn(new PresignedDownload("https://s3/download", NOW));

        materialService.download("ana", material.getId());

        assertThat(disposition.getValue().isAttachment()).isTrue();
    }

    @Test
    void a_link_has_nothing_to_download() {
        Material material = givenAVisible(CourseFixtures.link("Clase grabada"));

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.download("ana", material.getId()));

        assertThat(error.getCode()).isEqualTo("CRS_MATERIAL_WITHOUT_FILE");
    }

    @Test
    void material_the_reader_cannot_see_is_not_signed() {
        Material material = CourseFixtures.material(unit.getId(), pdf(), NOW);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        doThrow(new MaterialNotFoundException(material.getId()))
                .when(unitService).requireVisible("ana", material);

        ApiException error = assertThrows(ApiException.class,
                () -> materialService.download("ana", material.getId()));

        assertThat(error.getCode()).isEqualTo("CRS_MATERIAL_NOT_FOUND");
        verifyNoInteractions(fileStorage);
    }

    @Test
    void updating_a_material_turns_a_file_into_a_link() {
        Material material = givenAnExistingPdf();

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

    private static MaterialData pdf() {
        return new MaterialData("Tema 1", MaterialType.PDF, KEY, null, null, true);
    }

    private Material givenAnExistingPdf() {
        Material material = CourseFixtures.material(unit.getId(), pdf(), NOW);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        givenTheUnitIsWritable();
        return material;
    }

    private Material givenAVisible(MaterialData data) {
        Material material = CourseFixtures.material(unit.getId(), data, NOW);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        return material;
    }

    private void givenTheUnitIsWritable() {
        when(unitService.writable("juan", unit.getId())).thenReturn(unit);
    }

    private void givenTheFileWasUploaded(String contentType, long size) {
        when(fileStorage.describe(KEY)).thenReturn(Optional.of(new StoredObject(size, contentType)));
    }

    private void givenTheMaterialIsStored() {
        when(materialRepository.save(any(Material.class))).thenAnswer(call -> {
            Material material = call.getArgument(0);
            ReflectionTestUtils.setField(material, "id", UUID.randomUUID());
            return material;
        });
    }
}
