package io.github.smaykell.aulavirtual.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FiltersTest {

    @Test
    void without_a_search_the_pattern_matches_anything() {
        assertThat(Filters.containing(null)).isEqualTo("%");
        assertThat(Filters.containing("   ")).isEqualTo("%");
    }

    @Test
    void the_search_is_trimmed_lowercased_and_wrapped() {
        assertThat(Filters.containing("  Pérez ")).isEqualTo("%pérez%");
    }

    @Test
    void the_wildcards_typed_by_the_user_are_taken_literally() {
        assertThat(Filters.containing("50%_a\\b")).isEqualTo("%50\\%\\_a\\\\b%");
    }
}
