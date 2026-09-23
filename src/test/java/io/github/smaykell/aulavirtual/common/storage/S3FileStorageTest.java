package io.github.smaykell.aulavirtual.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;

class S3FileStorageTest {

    private static final String KEY = "courses/algebra/materials/tema-1.pdf";

    private final StorageProperties properties = new StorageProperties("http://localhost:9000",
            "us-east-1", "aula-virtual", "access", "secret", true,
            Duration.ofMinutes(15), Duration.ofMinutes(10));

    private final StorageConfig config = new StorageConfig();

    private final S3FileStorage storage = new S3FileStorage(config.s3Client(properties),
            config.s3Presigner(properties), properties);

    @Test
    void an_upload_url_points_to_the_key_in_the_bucket() {
        PresignedUpload upload = storage.presignUpload(KEY, "application/pdf", 2048);

        assertThat(upload.url()).startsWith("http://localhost:9000/aula-virtual/" + KEY + "?");
    }

    @Test
    void an_upload_signs_its_size_type_and_pending_tag() {
        PresignedUpload upload = storage.presignUpload(KEY, "application/pdf", 2048);

        assertThat(upload.headers()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "content-type", "application/pdf",
                "content-length", "2048",
                "x-amz-tagging", S3FileStorage.PENDING_TAG));
    }

    @Test
    void an_upload_url_expires_after_its_lifetime() {
        Instant before = Instant.now();

        PresignedUpload upload = storage.presignUpload(KEY, "application/pdf", 2048);

        assertThat(upload.expiresAt()).isBetween(before.plus(Duration.ofMinutes(15)),
                Instant.now().plus(Duration.ofMinutes(15)));
    }

    @Test
    void a_download_url_carries_the_disposition_and_its_own_lifetime() {
        Instant before = Instant.now();

        PresignedDownload download = storage.presignDownload(KEY,
                ContentDisposition.attachment().filename("tema-1.pdf").build());

        assertThat(download.url()).contains("response-content-disposition=attachment");
        assertThat(download.expiresAt()).isBetween(before.plus(Duration.ofMinutes(10)),
                Instant.now().plus(Duration.ofMinutes(10)));
    }
}
