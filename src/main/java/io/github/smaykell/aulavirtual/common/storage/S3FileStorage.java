package io.github.smaykell.aulavirtual.common.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Component
@RequiredArgsConstructor
class S3FileStorage implements FileStorage {

    static final String PENDING_TAG = "status=pending";
    private static final String HOST_HEADER = "host";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    @Override
    public PresignedUpload presignUpload(String key, String contentType, long size) {
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(request -> request
                .putObjectRequest(object -> object
                        .bucket(properties.bucket())
                        .key(key)
                        .contentType(contentType)
                        .contentLength(size)
                        .tagging(PENDING_TAG))
                .signatureDuration(properties.uploadTtl()));

        return new PresignedUpload(presigned.url().toString(),
                headersToSend(presigned.signedHeaders()), presigned.expiration());
    }

    @Override
    public PresignedDownload presignDownload(String key, ContentDisposition disposition) {
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(request -> request
                .getObjectRequest(object -> object
                        .bucket(properties.bucket())
                        .key(key)
                        .responseContentDisposition(disposition.toString()))
                .signatureDuration(properties.downloadTtl()));

        return new PresignedDownload(presigned.url().toString(), presigned.expiration());
    }

    @Override
    public Optional<StoredObject> describe(String key) {
        try {
            HeadObjectResponse head = s3Client.headObject(request -> request
                    .bucket(properties.bucket())
                    .key(key));
            return Optional.of(new StoredObject(head.contentLength(), head.contentType()));
        } catch (NoSuchKeyException missing) {
            return Optional.empty();
        }
    }

    @Override
    public void claim(String key) {
        s3Client.deleteObjectTagging(request -> request.bucket(properties.bucket()).key(key));
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(request -> request.bucket(properties.bucket()).key(key));
    }

    private static Map<String, String> headersToSend(Map<String, List<String>> signedHeaders) {
        return signedHeaders.entrySet().stream()
                .filter(header -> !header.getKey().equalsIgnoreCase(HOST_HEADER))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        header -> String.join(",", header.getValue())));
    }
}
