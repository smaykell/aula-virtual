package io.github.smaykell.aulavirtual.common.storage;

import java.util.Optional;
import org.springframework.http.ContentDisposition;

public interface FileStorage {

    PresignedUpload presignUpload(String key, String contentType, long size);

    PresignedDownload presignDownload(String key, ContentDisposition disposition);

    Optional<StoredObject> describe(String key);

    void claim(String key);

    void delete(String key);
}
