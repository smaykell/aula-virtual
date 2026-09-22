package io.github.smaykell.aulavirtual.course.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CourseError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Curso con id %s no encontrado"),
    OUT_OF_REACH(HttpStatus.FORBIDDEN, "No administras este curso"),
    ARCHIVED(HttpStatus.CONFLICT, "El curso está archivado; actívalo para poder modificarlo"),
    INVALID_DATES(HttpStatus.BAD_REQUEST,
            "La fecha de fin no puede ser anterior a la de inicio"),
    TEACHER_REQUIRED(HttpStatus.BAD_REQUEST, "Indica el docente titular del curso"),
    UNIT_NOT_FOUND(HttpStatus.NOT_FOUND, "Unidad con id %s no encontrada"),
    INVALID_UNIT_ORDER(HttpStatus.BAD_REQUEST,
            "El orden debe incluir todas las unidades del curso y ninguna repetida"),
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Material con id %s no encontrado"),
    MATERIAL_NEEDS_URL(HttpStatus.BAD_REQUEST,
            "Un material de tipo enlace necesita una URL externa"),
    MATERIAL_NEEDS_FILE(HttpStatus.BAD_REQUEST, "Un material de tipo %s necesita un archivo"),
    INVALID_INVITATION(HttpStatus.NOT_FOUND,
            "El código de invitación no corresponde a ningún curso"),
    STUDENT_REQUIRED(HttpStatus.FORBIDDEN, "Solo un estudiante puede inscribirse en un curso"),
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "Ya estás inscrito en este curso"),
    ENROLLMENT_PENDING(HttpStatus.CONFLICT,
            "Ya enviaste una solicitud a este curso y sigue pendiente"),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Matrícula con id %s no encontrada"),
    ENROLLMENT_NOT_PENDING(HttpStatus.CONFLICT, "Esa solicitud ya fue resuelta"),
    ENROLLMENT_NOT_ACTIVE(HttpStatus.CONFLICT, "Esa matrícula no está activa"),
    COURSE_NOT_OPEN(HttpStatus.CONFLICT, "Este curso ya no admite inscripciones"),
    SELF_REGISTRATION_CLOSED(HttpStatus.CONFLICT,
            "Las inscripciones por enlace están cerradas"),
    SELF_REGISTRATION_DOCUMENT(HttpStatus.BAD_REQUEST,
            "Para inscribirte por el enlace necesitas tu DNI o tu carné de extranjería; "
                    + "con pasaporte, pide tu cuenta al centro"),
    ACCOUNT_ALREADY_REGISTERED(HttpStatus.CONFLICT,
            "Ya tienes una cuenta en el aula virtual. Inicia sesión para unirte al curso.");

    public static final String PREFIX = "CRS";

    private final HttpStatus status;
    private final String message;

    CourseError(HttpStatus status, String message) {
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
