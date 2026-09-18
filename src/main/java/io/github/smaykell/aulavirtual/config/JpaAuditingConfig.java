package io.github.smaykell.aulavirtual.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa el rellenado automatico de {@code createdAt} y {@code updatedAt}
 * en las entidades que extienden {@code BaseEntity}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
