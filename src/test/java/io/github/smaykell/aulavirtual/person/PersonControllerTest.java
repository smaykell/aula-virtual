package io.github.smaykell.aulavirtual.person;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.smaykell.aulavirtual.common.web.ApiErrorWriter;
import io.github.smaykell.aulavirtual.config.ClockConfig;
import io.github.smaykell.aulavirtual.config.CorsProperties;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.JwtAuthenticationFilter;
import io.github.smaykell.aulavirtual.security.JwtProperties;
import io.github.smaykell.aulavirtual.security.JwtService;
import io.github.smaykell.aulavirtual.security.PersonProfiles;
import io.github.smaykell.aulavirtual.security.RestAccessDeniedHandler;
import io.github.smaykell.aulavirtual.security.RestAuthenticationEntryPoint;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.security.SecurityConfig;
import java.time.LocalDate;
import java.util.Optional;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PersonController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiErrorWriter.class,
        ClockConfig.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
class PersonControllerTest {

    private static final UUID PERSON = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PersonService personService;

    @MockitoBean
    private PersonProfiles personProfiles;

    @Test
    void an_admin_finds_a_person_by_its_document_and_sees_what_it_already_is() throws Exception {
        when(personService.findByDocument(DocumentType.DNI, "45678912"))
                .thenReturn(Optional.of(person()));
        when(personProfiles.rolesOf(PERSON)).thenReturn(Set.of(Role.STUDENT));

        mockMvc.perform(get("/persons/$byDocument")
                        .param("documentType", "DNI")
                        .param("documentNumber", "45678912")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.firstName").value("Ana Maria"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
    }

    @Test
    void an_unknown_document_answers_404_with_its_own_code() throws Exception {
        when(personService.findByDocument(DocumentType.DNI, "45678912"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/persons/$byDocument")
                        .param("documentType", "DNI")
                        .param("documentNumber", "45678912")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRS_DOCUMENT_NOT_FOUND"));
    }

    @Test
    void a_teacher_cannot_look_up_people() throws Exception {
        mockMvc.perform(get("/persons/$byDocument")
                        .param("documentType", "DNI")
                        .param("documentNumber", "45678912")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(Role.TEACHER)))
                .andExpect(status().isForbidden());

        verify(personService, never()).findByDocument(any(), any());
    }

    @Test
    void without_a_token_it_answers_401() throws Exception {
        mockMvc.perform(get("/persons/$byDocument")
                        .param("documentType", "DNI")
                        .param("documentNumber", "45678912"))
                .andExpect(status().isUnauthorized());
    }

    private PersonResponse person() {
        return new PersonResponse(PERSON, DocumentType.DNI, "45678912", "Ana Maria", "Lopez Diaz",
                LocalDate.of(1990, 5, 20), Sex.FEMALE, null);
    }

    private String bearerFor(Role role) {
        return "Bearer " + jwtService.issueToken("ana", Role.grantedAuthoritiesOf(Set.of(role)));
    }
}
