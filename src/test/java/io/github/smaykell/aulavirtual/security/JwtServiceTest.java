package io.github.smaykell.aulavirtual.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String ISSUER = "aula-virtual";
    private static final String SECRET = base64("clave-de-pruebas-con-mas-de-32-bytes");
    private static final String OTHER_SECRET = base64("otra-clave-distinta-de-mas-de-32-bytes");

    private final JwtService jwtService = serviceWith(SECRET, ISSUER);

    @Test
    void issues_a_token_that_it_accepts_back() {
        String token = jwtService.issueToken("ana@aula.test", List.of("ROLE_TEACHER"));

        Claims claims = jwtService.verify(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("ana@aula.test");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void keeps_the_authorities_in_the_token() {
        String token = jwtService.issueToken("ana@aula.test",
                List.of("ROLE_TEACHER", "ROLE_COORDINATOR"));

        Claims claims = jwtService.verify(token).getPayload();

        assertThat(jwtService.authoritiesOf(claims))
                .containsExactly("ROLE_TEACHER", "ROLE_COORDINATOR");
    }

    @Test
    void returns_no_authorities_when_the_token_carries_none() {
        String token = jwtService.issueToken("ana@aula.test", List.of());

        Claims claims = jwtService.verify(token).getPayload();

        assertThat(jwtService.authoritiesOf(claims)).isEmpty();
    }

    @Test
    void rejects_an_expired_token() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        JwtService serviceInThePast = new JwtService(propertiesOf(SECRET, ISSUER), twoHoursAgo);
        String expiredToken = serviceInThePast.issueToken("ana@aula.test", List.of());

        assertThatThrownBy(() -> jwtService.verify(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejects_a_token_signed_with_another_key() {
        JwtService foreignService = serviceWith(OTHER_SECRET, ISSUER);
        String foreignToken = foreignService.issueToken("intruso@aula.test", List.of("ROLE_ADMIN"));

        assertThatThrownBy(() -> jwtService.verify(foreignToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void rejects_a_token_from_another_issuer() {
        JwtService otherIssuer = serviceWith(SECRET, "otra-aplicacion");
        String token = otherIssuer.issueToken("ana@aula.test", List.of());

        assertThatThrownBy(() -> jwtService.verify(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejects_a_tampered_token() {
        String token = jwtService.issueToken("ana@aula.test", List.of("ROLE_STUDENT"));
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtService.verify(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void fails_to_start_with_a_secret_shorter_than_256_bits() {
        JwtProperties shortSecret = propertiesOf(base64("muy-corto"), ISSUER);

        assertThatThrownBy(() -> new JwtService(shortSecret, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 32 bytes");
    }

    @Test
    void fails_to_start_with_a_secret_that_is_not_base64() {
        JwtProperties invalidSecret = propertiesOf("no-es-base64-valido-!!!", ISSUER);

        assertThatThrownBy(() -> new JwtService(invalidSecret, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    private static JwtService serviceWith(String secret, String issuer) {
        return new JwtService(propertiesOf(secret, issuer), Clock.systemUTC());
    }

    private static JwtProperties propertiesOf(String secret, String issuer) {
        return new JwtProperties(secret, issuer, Duration.ofHours(1));
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(UTF_8));
    }
}
