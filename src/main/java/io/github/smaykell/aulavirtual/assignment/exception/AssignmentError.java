package io.github.smaykell.aulavirtual.assignment.exception;

import io.github.smaykell.aulavirtual.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AssignmentError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "Tarea con id %s no encontrada"),
    ONLY_STUDENTS_SUBMIT(HttpStatus.FORBIDDEN,
            "Solo un estudiante matriculado puede entregar una tarea"),
    EMPTY_SUBMISSION(HttpStatus.BAD_REQUEST, "La entrega necesita un texto o algún adjunto"),
    DEADLINE_PASSED(HttpStatus.CONFLICT,
            "La fecha límite ya pasó y esta tarea no admite entregas tardías"),
    ALREADY_GRADED(HttpStatus.CONFLICT,
            "La nota de esta entrega ya se devolvió y la entrega no admite cambios"),
    HAS_WORK(HttpStatus.CONFLICT,
            "La tarea ya tiene entregas o notas y no se puede eliminar"),
    SUBMISSION_OUT_OF_REACH(HttpStatus.FORBIDDEN, "Esa entrega no es tuya"),
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Adjunto con id %s no encontrado"),
    ATTACHMENT_IS_LINK(HttpStatus.CONFLICT,
            "Este adjunto es un enlace y no tiene archivo que descargar"),
    ATTACHMENT_WITHOUT_SOURCE(HttpStatus.BAD_REQUEST,
            "Un adjunto de archivo necesita el archivo subido y uno de enlace, su dirección"),
    FILE_NOT_UPLOADED(HttpStatus.NOT_FOUND,
            "El archivo no llegó a subirse o su enlace de subida caducó; vuelve a subirlo"),
    FOREIGN_FILE(HttpStatus.BAD_REQUEST, "El archivo indicado no se subió para esta tarea"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "Un adjunto admite archivos de hasta %s MB"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "No se admiten adjuntos de tipo %s");

    public static final String PREFIX = "ASG";

    private final HttpStatus status;
    private final String message;

    AssignmentError(HttpStatus status, String message) {
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
