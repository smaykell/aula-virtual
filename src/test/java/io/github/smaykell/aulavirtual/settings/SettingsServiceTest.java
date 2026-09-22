package io.github.smaykell.aulavirtual.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.settings.dto.SettingsResponse;
import io.github.smaykell.aulavirtual.settings.dto.UpdateSettingsRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private SettingsRepository settingsRepository;

    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService(settingsRepository);
    }

    @Test
    void the_seeded_row_identifies_the_students_by_their_document() {
        givenTheStoredSettings(StudentIdentifier.DOCUMENT_NUMBER, false);

        SettingsResponse current = settingsService.current();

        assertThat(current.studentIdentifier()).isEqualTo(StudentIdentifier.DOCUMENT_NUMBER);
        assertThat(current.selfRegistrationEnabled()).isFalse();
    }

    @Test
    void updating_rewrites_the_single_row_instead_of_adding_another_one() {
        Settings stored = givenTheStoredSettings(StudentIdentifier.DOCUMENT_NUMBER, false);

        SettingsResponse updated = settingsService.update(
                new UpdateSettingsRequest(StudentIdentifier.EMAIL, true));

        assertThat(updated.studentIdentifier()).isEqualTo(StudentIdentifier.EMAIL);
        assertThat(updated.selfRegistrationEnabled()).isTrue();
        assertThat(stored.getStudentIdentifier()).isEqualTo(StudentIdentifier.EMAIL);
    }

    @Test
    void self_registration_is_closed_until_somebody_opens_it() {
        givenTheStoredSettings(StudentIdentifier.DOCUMENT_NUMBER, false);

        assertThat(settingsService.current().selfRegistrationEnabled()).isFalse();
    }

    @Test
    void a_database_without_the_seeded_row_is_a_broken_installation() {
        when(settingsRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> settingsService.current())
                .isInstanceOf(IllegalStateException.class);
    }

    private Settings givenTheStoredSettings(StudentIdentifier identifier, boolean selfRegistration) {
        Settings settings = new Settings();
        ReflectionTestUtils.setField(settings, "studentIdentifier", identifier);
        ReflectionTestUtils.setField(settings, "selfRegistrationEnabled", selfRegistration);
        when(settingsRepository.findAll()).thenReturn(List.of(settings));
        return settings;
    }
}
