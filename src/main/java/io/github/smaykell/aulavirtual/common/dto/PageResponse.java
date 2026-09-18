package io.github.smaykell.aulavirtual.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Envoltura estable de paginacion para el frontend.
 *
 * <p>Existe para no serializar directamente {@link Page}, cuyo JSON depende de
 * detalles internos de Spring Data y cambia entre versiones.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> de(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    /** Pagina de entidades convertida a una pagina de DTOs. */
    public static <E, T> PageResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return de(page.map(mapper));
    }
}
