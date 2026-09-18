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
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String ROLES_CLAIM = "roles";

    private static final int MINIMUM_HS256_KEY_BYTES = 32;
    private static final String KEY_GENERATION_HINT = "Genérala con: openssl rand -base64 32";

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = signingKeyFrom(properties.secret());
    }

    public String issueToken(String subject, Collection<String> authorities) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiration())))
                .claim(ROLES_CLAIM, List.copyOf(authorities))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> verify(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token);
    }

    public List<String> authoritiesOf(Claims claims) {
        Object claim = claims.get(ROLES_CLAIM);
        if (!(claim instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }

    private static SecretKey signingKeyFrom(String base64Secret) {
        byte[] keyBytes = decode(base64Secret);
        if (keyBytes.length < MINIMUM_HS256_KEY_BYTES) {
            throw new IllegalStateException(
                    "app.security.jwt.secret debe tener al menos %d bytes para HS256, tiene %d. %s"
                            .formatted(MINIMUM_HS256_KEY_BYTES, keyBytes.length,
                                    KEY_GENERATION_HINT));
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static byte[] decode(String base64Secret) {
        try {
            return Decoders.BASE64.decode(base64Secret);
        } catch (DecodingException ex) {
            throw new IllegalStateException(
                    "app.security.jwt.secret debe estar codificado en Base64. %s"
                            .formatted(KEY_GENERATION_HINT), ex);
        }
    }
}
