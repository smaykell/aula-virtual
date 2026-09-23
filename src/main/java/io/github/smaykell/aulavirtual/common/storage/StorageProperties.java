package io.github.smaykell.aulavirtual.common.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        boolean pathStyle,
        @NotNull Duration uploadTtl,
        @NotNull Duration downloadTtl) {

    public Optional<URI> endpointOverride() {
        return endpoint == null || endpoint.isBlank()
                ? Optional.empty()
                : Optional.of(URI.create(endpoint));
    }
}
