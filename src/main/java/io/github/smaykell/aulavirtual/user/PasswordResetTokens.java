package io.github.smaykell.aulavirtual.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
class PasswordResetTokens {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    String next() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    String hashOf(String token) {
        return HexFormat.of().formatHex(sha256().digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("La JVM no trae SHA-256", impossible);
        }
    }
}
