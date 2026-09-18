package io.github.smaykell.aulavirtual.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class, GlobalExceptionHandlerTest.ProbeController.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void an_unmapped_path_answers_404_and_not_500() throws Exception {
        mockMvc.perform(authenticated(get("/no-existe-en-absoluto")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void a_wrong_http_method_answers_405_and_not_500() throws Exception {
        mockMvc.perform(authenticated(post("/items/search")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void a_missing_required_parameter_answers_400_and_not_500() throws Exception {
        mockMvc.perform(authenticated(get("/items/search")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void an_unparseable_path_variable_answers_400_and_not_500() throws Exception {
        mockMvc.perform(authenticated(get("/items/no-es-un-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void an_invalid_body_answers_400_with_the_field_that_failed() throws Exception {
        mockMvc.perform(authenticated(post("/items"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La petición contiene campos inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void a_malformed_body_answers_400() throws Exception {
        mockMvc.perform(authenticated(post("/items"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El cuerpo de la petición no es un JSON válido"));
    }

    @Test
    void a_domain_exception_keeps_its_own_status() throws Exception {
        mockMvc.perform(authenticated(get("/items/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El recurso solicitado no existe"))
                .andExpect(jsonPath("$.code").value("GEN_RESOURCE_NOT_FOUND"));
    }

    @Test
    void an_upload_over_the_limit_answers_413_with_its_own_message() throws Exception {
        mockMvc.perform(authenticated(get("/items/too-large")))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.message")
                        .value("La petición excede el tamaño máximo permitido"));
    }

    @Test
    void an_unexpected_failure_answers_500_without_leaking_details() throws Exception {
        mockMvc.perform(authenticated(get("/boom")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.message").value(
                        "Ha ocurrido un error inesperado. Reporta el traceId al administrador."));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issueToken("ana@aula.test", List.of("ROLE_TEACHER")));
    }

    @RestController
    @Validated
    static class ProbeController {

        @GetMapping("/items/search")
        String search(@RequestParam String query) {
            return query;
        }

        @GetMapping("/items/too-large")
        String tooLarge() {
            throw new MaxUploadSizeExceededException(1024);
        }

        @GetMapping("/items/{id}")
        String byId(@PathVariable UUID id) {
            throw new ApiException(CommonError.RESOURCE_NOT_FOUND);
        }

        @PostMapping("/items")
        String create(@RequestBody @Valid NewItem item) {
            return item.name();
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("detalle interno que no debe salir");
        }

        record NewItem(@NotBlank String name) {
        }
    }
}
