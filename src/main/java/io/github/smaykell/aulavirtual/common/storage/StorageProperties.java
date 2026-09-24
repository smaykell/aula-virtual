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
        String accessKey,
        String secretKey,
        boolean pathStyle,
        @NotNull Duration uploadTtl,
        @NotNull Duration downloadTtl) {

    public Optional<URI> endpointOverride() {
        return hasText(endpoint)
                ? Optional.of(URI.create(endpoint))
                : Optional.empty();
    }

    public boolean usesStaticCredentials() {
        return hasText(accessKey) && hasText(secretKey);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
