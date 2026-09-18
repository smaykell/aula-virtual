package io.github.smaykell.aulavirtual.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base de toda entidad persistente: identificador UUID y marcas de auditoria.
 *
 * <p>El UUID lo genera Hibernate al persistir, no la base de datos, para que la
 * entidad tenga identidad antes del INSERT.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean esNueva() {
        return id == null;
    }

    /**
     * Igualdad por identificador. Dos entidades sin persistir nunca son iguales,
     * que es lo correcto mientras no tienen identidad propia.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BaseEntity otra)) {
            return false;
        }
        return id != null && id.equals(otra.id);
    }

    /**
     * Constante por clase: el hash no puede cambiar cuando Hibernate asigna el id
     * a una entidad que ya esta dentro de un HashSet.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
