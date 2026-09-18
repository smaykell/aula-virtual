package io.github.smaykell.aulavirtual.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI aulaVirtualOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aula Virtual API")
                        .version("v1")
                        .description("""
                                API REST del aula virtual. El frontend se sirve desde otro repositorio.

                                Todos los endpoints requieren un token JWT en la cabecera
                                Authorization, salvo /auth/login, /actuator/health y la propia
                                documentacion. El token se obtiene en POST /auth/login.""")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT emitido por el aula virtual.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
