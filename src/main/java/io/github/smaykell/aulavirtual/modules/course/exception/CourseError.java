package io.github.smaykell.aulavirtual.modules.course.exception;

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
    MATERIAL_NEEDS_FILE(HttpStatus.BAD_REQUEST, "Un material de tipo %s necesita un archivo");

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
