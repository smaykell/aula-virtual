package io.github.smaykell.aulavirtual.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepcion de negocio que ya sabe con que codigo HTTP debe responderse.
 *
 * <p>El mensaje de una {@code ApiException} se envia al cliente tal cual, asi que
 * debe estar escrito para un usuario final y no contener detalles internos.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public ApiException(HttpStatus status, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.status = status;
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);
    }

    public static ApiException solicitudInvalida(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
