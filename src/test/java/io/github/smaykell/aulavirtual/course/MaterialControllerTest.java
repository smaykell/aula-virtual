package io.github.smaykell.aulavirtual.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.course.dto.MaterialData;
import io.github.smaykell.aulavirtual.course.dto.MaterialResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import java.time.Instant;
import java.util.Set;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MaterialController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class MaterialControllerTest {

    private static final UUID UNIT = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private MaterialService materialService;

    @Test
    void without_a_token_publishing_material_answers_401() throws Exception {
        mockMvc.perform(post("/units/{unitId}/materials", UNIT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody("PDF")))
                .andExpect(status().isUnauthorized());

        verify(materialService, never()).create(any(), any(), any());
    }

    @Test
    void a_student_cannot_publish_material() throws Exception {
        mockMvc.perform(post("/units/{unitId}/materials", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody("PDF")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GEN_ACCESS_DENIED"));

        verify(materialService, never()).create(any(), any(), any());
    }

    @Test
    void a_teacher_publishes_material_and_gets_a_201() throws Exception {
        when(materialService.create(eq("ana"), eq(UNIT), any(MaterialData.class)))
                .thenReturn(aMaterial());

        mockMvc.perform(post("/units/{unitId}/materials", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody("PDF")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tema 1"))
                .andExpect(jsonPath("$.type").value("PDF"))
                .andExpect(jsonPath("$.visible").value(true));
    }

    @Test
    void material_of_an_unknown_type_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/units/{unitId}/materials", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody("HOLOGRAMA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GEN_MALFORMED_JSON"));

        verify(materialService, never()).create(any(), any(), any());
    }

    @Test
    void material_without_visibility_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/units/{unitId}/materials", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Tema 1\", \"type\": \"PDF\","
                                + " \"storageKey\": \"courses/algebra/tema-1.pdf\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("visible"));

        verify(materialService, never()).create(any(), any(), any());
    }

    @Test
    void a_teacher_replaces_the_material_of_a_unit() throws Exception {
        UUID materialId = UUID.randomUUID();
        when(materialService.update(eq("ana"), eq(materialId), any(MaterialData.class)))
                .thenReturn(aMaterial());

        mockMvc.perform(put("/materials/{id}", materialId)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody("PDF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageKey").value("courses/algebra/tema-1.pdf"));
    }

    @Test
    void deleting_material_answers_204() throws Exception {
        UUID materialId = UUID.randomUUID();

        mockMvc.perform(delete("/materials/{id}", materialId)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isNoContent());

        verify(materialService).delete("ana", materialId);
    }

    private MaterialResponse aMaterial() {
        return new MaterialResponse(UUID.randomUUID(), UNIT, "Tema 1", MaterialType.PDF,
                "courses/algebra/tema-1.pdf", null, Instant.EPOCH, true);
    }

    private String fileBody(String type) {
        return """
                {"title": "Tema 1", "type": "%s",
                 "storageKey": "courses/algebra/tema-1.pdf", "visible": true}
                """.formatted(type);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
