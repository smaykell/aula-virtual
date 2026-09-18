package io.github.smaykell.aulavirtual.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parametros de firma y vigencia de los tokens JWT.
 *
 * @param secret     clave de firma en Base64, de al menos 256 bits (32 bytes).
 *                   Se genera con {@code openssl rand -base64 32}.
 * @param issuer     emisor que se escribe y se exige en cada token
 * @param expiration vigencia del token desde su emision
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration expiration) {
}
