package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.modules.course.dto.InvitationResponse;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class Invitations {

    // Sin I, O, 0 ni 1: el codigo se dicta en voz alta y se teclea a mano.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;
    private static final int ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final CourseRepository courseRepository;
    private final CourseProperties properties;

    String nextCode() {
        return IntStream.range(0, ATTEMPTS)
                .mapToObj(attempt -> candidate())
                .filter(code -> !courseRepository.existsByInvitationCode(code))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo generar un código de invitación libre"));
    }

    InvitationResponse of(String code) {
        return new InvitationResponse(code, properties.invitationBaseUrl() + "/" + code);
    }

    private String candidate() {
        return IntStream.range(0, LENGTH)
                .mapToObj(position -> String.valueOf(ALPHABET.charAt(
                        random.nextInt(ALPHABET.length()))))
                .collect(Collectors.joining());
    }
}
