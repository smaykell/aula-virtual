package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ExamError implements ErrorCode {

    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Pregunta con id %s no encontrada"),
    ANSWERS_REQUIRE_STAFF(HttpStatus.FORBIDDEN,
            "Solo quien dicta o administra el curso puede ver las preguntas con sus respuestas"),
    CHOICES_OUT_OF_RANGE(HttpStatus.BAD_REQUEST,
            "Una pregunta de opciones necesita entre %s y %s opciones"),
    ONE_CORRECT_CHOICE(HttpStatus.BAD_REQUEST,
            "Una pregunta de opción única necesita exactamente una opción correcta"),
    SOME_CORRECT_CHOICE(HttpStatus.BAD_REQUEST,
            "Una pregunta de opción múltiple necesita al menos una opción correcta"),
    TRUTH_REQUIRED(HttpStatus.BAD_REQUEST, "Indica si el enunciado es verdadero o falso"),
    CHOICES_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
            "Las preguntas de verdadero o falso y las de respuesta corta no llevan opciones"),
    QUESTION_IN_USE(HttpStatus.CONFLICT,
            "La pregunta forma parte de algún examen y no se puede eliminar"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Examen con id %s no encontrado"),
    INVALID_WINDOW(HttpStatus.BAD_REQUEST,
            "La hora de cierre del examen debe ser posterior a la de apertura"),
    REPEATED_QUESTION(HttpStatus.BAD_REQUEST, "La pregunta %s aparece dos veces en el examen"),
    HAS_WORK(HttpStatus.CONFLICT, "El examen ya tiene intentos o notas y no se puede eliminar"),
    HAS_ATTEMPTS(HttpStatus.CONFLICT,
            "El examen ya tiene intentos y sus preguntas no se pueden cambiar"),
    QUESTION_LOCKED(HttpStatus.CONFLICT,
            "La pregunta forma parte de un examen que ya se rindió y no se puede modificar"),
    ONLY_STUDENTS_TAKE(HttpStatus.FORBIDDEN,
            "Solo un estudiante matriculado puede rendir un examen"),
    NOT_OPEN(HttpStatus.CONFLICT,
            "El examen solo se puede rendir entre su hora de apertura y la de cierre"),
    WITHOUT_QUESTIONS(HttpStatus.CONFLICT, "El examen todavía no tiene preguntas"),
    NO_ATTEMPTS_LEFT(HttpStatus.CONFLICT, "Ya usaste todos los intentos de este examen"),
    GRADE_RETURNED(HttpStatus.CONFLICT,
            "La nota de este examen ya se devolvió y no admite otro intento"),
    ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "Intento con id %s no encontrado"),
    ATTEMPT_OUT_OF_REACH(HttpStatus.FORBIDDEN, "Ese intento no es tuyo"),
    ATTEMPT_CLOSED(HttpStatus.CONFLICT, "El intento ya se entregó o se le acabó el tiempo"),
    FOREIGN_OPTION(HttpStatus.BAD_REQUEST, "La opción elegida no es de esta pregunta"),
    ONE_OPTION_ONLY(HttpStatus.BAD_REQUEST, "Esta pregunta admite una sola opción");

    public static final String PREFIX = "EXM";

    private final HttpStatus status;
    private final String message;

    ExamError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String prefix() {
        return PREFIX;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
