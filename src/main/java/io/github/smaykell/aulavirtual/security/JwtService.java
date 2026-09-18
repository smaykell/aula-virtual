package io.github.smaykell.aulavirtual.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Emision y verificacion de tokens JWT firmados con HS256.
 *
 * <p>El servicio no sabe nada de usuarios: recibe un subject y una lista de
 * authorities y produce un token. Quien decide que ese subject existe y que esas
 * authorities le corresponden es el modulo de usuarios, cuando exista.
 */
@Service
public class JwtService {

    /** Claim donde viajan las authorities, ya con su prefijo (por ejemplo ROLE_DOCENTE). */
    public static final String CLAIM_ROLES = "roles";

    private static final int BYTES_MINIMOS_HS256 = 32;

    private final JwtProperties propiedades;
    private final SecretKey clave;
    private final Clock reloj;

    /**
     * Constructor que usa el contenedor. La anotacion es necesaria: con dos
     * constructores y ninguno marcado, Spring no puede elegir y busca uno vacio.
     */
    @Autowired
    public JwtService(JwtProperties propiedades) {
        this(propiedades, Clock.systemUTC());
    }

    /** Constructor visible para los tests, que necesitan controlar el tiempo. */
    JwtService(JwtProperties propiedades, Clock reloj) {
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.clave = construirClave(propiedades.secret());
    }

    /**
     * Emite un token firmado para el subject dado.
     *
     * @param subject     identificador del titular del token
     * @param authorities authorities concedidas, tal como las evaluara Spring Security
     */
    public String generarToken(String subject, Collection<String> authorities) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .issuer(propiedades.issuer())
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(propiedades.expiration())))
                .claim(CLAIM_ROLES, List.copyOf(authorities))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifica firma, emisor y vigencia del token.
     *
     * @throws JwtException si el token es invalido, ajeno o ha expirado
     */
    public Jws<Claims> validar(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(propiedades.issuer())
                .clock(() -> Date.from(reloj.instant()))
                .build()
                .parseSignedClaims(token);
    }

    /** Authorities del token, o lista vacia si el claim no viene o no es una lista. */
    public List<String> extraerAuthorities(Claims claims) {
        Object valor = claims.get(CLAIM_ROLES);
        if (!(valor instanceof Collection<?> coleccion)) {
            return List.of();
        }
        return coleccion.stream().map(String::valueOf).toList();
    }

    private static SecretKey construirClave(String secretBase64) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretBase64);
        } catch (DecodingException ex) {
            throw new IllegalStateException(
                    "app.security.jwt.secret debe estar codificado en Base64. "
                            + "Genera uno con: openssl rand -base64 32", ex);
        }
        if (bytes.length < BYTES_MINIMOS_HS256) {
            throw new IllegalStateException(
                    "app.security.jwt.secret debe tener al menos %d bytes para HS256, tiene %d. "
                            .formatted(BYTES_MINIMOS_HS256, bytes.length)
                            + "Genera uno con: openssl rand -base64 32");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
