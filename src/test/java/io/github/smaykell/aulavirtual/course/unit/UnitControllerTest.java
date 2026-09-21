package io.github.smaykell.aulavirtual.course.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.course.unit.dto.MaterialResponse;
import io.github.smaykell.aulavirtual.course.unit.dto.ReorderUnitsRequest;
import io.github.smaykell.aulavirtual.course.unit.dto.UnitData;
import io.github.smaykell.aulavirtual.course.unit.dto.UnitResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(UnitController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class UnitControllerTest {

    private static final UUID COURSE = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UnitService unitService;

    @Test
    void without_a_token_the_units_of_a_course_answer_401() throws Exception {
        mockMvc.perform(get("/courses/{courseId}/units", COURSE))
                .andExpect(status().isUnauthorized());

        verify(unitService, never()).list(any(), any());
    }

    @Test
    void a_student_reads_the_units_and_the_service_decides_what_it_sees() throws Exception {
        when(unitService.list("ana", COURSE)).thenReturn(List.of(aUnit()));

        mockMvc.perform(get("/courses/{courseId}/units", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT)))
                .andExpect(status().isOk());

        verify(unitService).list("ana", COURSE);
    }

    @Test
    void a_student_cannot_add_units() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/units", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Semana 1\"}"))
                .andExpect(status().isForbidden());

        verify(unitService, never()).create(any(), any(), any());
    }

    @Test
    void the_units_of_a_course_come_with_their_material() throws Exception {
        when(unitService.list("ana", COURSE)).thenReturn(List.of(aUnit()));

        mockMvc.perform(get("/courses/{courseId}/units", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Semana 1"))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].materials[0].title").value("Tema 1"));
    }

    @Test
    void a_teacher_adds_a_unit_and_gets_a_201() throws Exception {
        when(unitService.create(eq("ana"), eq(COURSE), any(UnitData.class))).thenReturn(aUnit());

        mockMvc.perform(post("/courses/{courseId}/units", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Semana 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Semana 1"));
    }

    @Test
    void a_unit_without_a_title_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/units", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        verify(unitService, never()).create(any(), any(), any());
    }

    @Test
    void the_order_of_the_units_travels_in_the_dollar_operation() throws Exception {
        UUID first = UUID.randomUUID();
        when(unitService.reorder(eq("ana"), eq(COURSE), any(ReorderUnitsRequest.class)))
                .thenReturn(List.of(aUnit()));

        mockMvc.perform(post("/courses/{courseId}/units/$reorder", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitIds\": [\"%s\"]}".formatted(first)))
                .andExpect(status().isOk());

        verify(unitService).reorder(eq("ana"), eq(COURSE), any(ReorderUnitsRequest.class));
    }

    @Test
    void an_empty_order_does_not_reach_the_service() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/units/$reorder", COURSE)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("unitIds"));

        verify(unitService, never()).reorder(any(), any(), any());
    }

    @Test
    void deleting_a_unit_answers_204() throws Exception {
        UUID unitId = UUID.randomUUID();

        mockMvc.perform(delete("/units/{id}", unitId)
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isNoContent());

        verify(unitService).delete("ana", unitId);
    }

    private UnitResponse aUnit() {
        UUID unitId = UUID.randomUUID();
        return new UnitResponse(unitId, COURSE, "Semana 1", 1,
                List.of(new MaterialResponse(UUID.randomUUID(), unitId, "Tema 1",
                        MaterialType.PDF, "courses/algebra/tema-1.pdf", null, Instant.EPOCH,
                        true)));
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
