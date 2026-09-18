package io.github.smaykell.aulavirtual.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion de CORS para el frontend, que vive en otro repositorio y en
 * otro origen. Se ajusta con la variable de entorno {@code CORS_ALLOWED_ORIGINS}.
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @NotEmpty List<String> allowedOrigins,
        @NotEmpty List<String> allowedMethods,
        @NotEmpty List<String> allowedHeaders,
        boolean allowCredentials,
        long maxAge) {
}
