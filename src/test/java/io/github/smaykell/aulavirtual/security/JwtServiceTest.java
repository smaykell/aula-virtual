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

    private static final String EMISOR = "aula-virtual";
    private static final String SECRETO = base64("clave-de-pruebas-con-mas-de-32-bytes");
    private static final String OTRO_SECRETO = base64("otra-clave-distinta-de-mas-de-32-bytes");

    private final JwtService jwtService = new JwtService(propiedades(SECRETO, EMISOR));

    @Test
    void emite_un_token_que_el_mismo_servicio_acepta() {
        String token = jwtService.generarToken("ana@aula.test", List.of("ROLE_DOCENTE"));

        Claims claims = jwtService.validar(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("ana@aula.test");
        assertThat(claims.getIssuer()).isEqualTo(EMISOR);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void conserva_las_authorities_en_el_token() {
        String token = jwtService.generarToken("ana@aula.test",
                List.of("ROLE_DOCENTE", "ROLE_COORDINADOR"));

        Claims claims = jwtService.validar(token).getPayload();

        assertThat(jwtService.extraerAuthorities(claims))
                .containsExactly("ROLE_DOCENTE", "ROLE_COORDINADOR");
    }

    @Test
    void devuelve_authorities_vacias_cuando_el_token_no_trae_ninguna() {
        String token = jwtService.generarToken("ana@aula.test", List.of());

        Claims claims = jwtService.validar(token).getPayload();

        assertThat(jwtService.extraerAuthorities(claims)).isEmpty();
    }

    @Test
    void rechaza_un_token_expirado() {
        Clock hace_dos_horas = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        JwtService servicioEnElPasado = new JwtService(propiedades(SECRETO, EMISOR), hace_dos_horas);
        String tokenVencido = servicioEnElPasado.generarToken("ana@aula.test", List.of());

        assertThatThrownBy(() -> jwtService.validar(tokenVencido))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rechaza_un_token_firmado_con_otra_clave() {
        JwtService servicioAjeno = new JwtService(propiedades(OTRO_SECRETO, EMISOR));
        String tokenAjeno = servicioAjeno.generarToken("intruso@aula.test", List.of("ROLE_ADMIN"));

        assertThatThrownBy(() -> jwtService.validar(tokenAjeno))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void rechaza_un_token_de_otro_emisor() {
        JwtService otroEmisor = new JwtService(propiedades(SECRETO, "otra-aplicacion"));
        String token = otroEmisor.generarToken("ana@aula.test", List.of());

        assertThatThrownBy(() -> jwtService.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechaza_un_token_manipulado() {
        String token = jwtService.generarToken("ana@aula.test", List.of("ROLE_ESTUDIANTE"));
        String manipulado = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtService.validar(manipulado)).isInstanceOf(JwtException.class);
    }

    @Test
    void no_arranca_con_un_secreto_mas_corto_que_256_bits() {
        JwtProperties secretoCorto = propiedades(base64("muy-corto"), EMISOR);

        assertThatThrownBy(() -> new JwtService(secretoCorto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 32 bytes");
    }

    @Test
    void no_arranca_con_un_secreto_que_no_es_base64() {
        JwtProperties secretoInvalido = propiedades("no-es-base64-valido-!!!", EMISOR);

        assertThatThrownBy(() -> new JwtService(secretoInvalido))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    private static JwtProperties propiedades(String secreto, String emisor) {
        return new JwtProperties(secreto, emisor, Duration.ofHours(1));
    }

    private static String base64(String valor) {
        return Base64.getEncoder().encodeToString(valor.getBytes(UTF_8));
    }
}
