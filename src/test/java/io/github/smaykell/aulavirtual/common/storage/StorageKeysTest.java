package io.github.smaykell.aulavirtual.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorageKeysTest {

    @Test
    void a_file_name_loses_accents_spaces_and_symbols() {
        assertThat(StorageKeys.safeFileName("  Sesión 1: Introducción (v2).PDF "))
                .isEqualTo("Sesion-1-Introduccion-v2.pdf");
    }

    @Test
    void a_file_name_cannot_climb_out_of_its_folder() {
        assertThat(StorageKeys.safeFileName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(StorageKeys.safeFileName("C:\\Users\\ana\\tema.pdf")).isEqualTo("tema.pdf");
    }

    @Test
    void a_file_name_without_letters_gets_a_generic_name() {
        assertThat(StorageKeys.safeFileName("¿?.pdf")).isEqualTo("archivo.pdf");
    }

    @Test
    void a_long_file_name_is_cut_but_keeps_its_extension() {
        String name = StorageKeys.safeFileName("a".repeat(300) + ".docx");

        assertThat(name).hasSize(95).endsWith(".docx");
    }

    @Test
    void the_file_name_is_the_last_segment_of_the_key() {
        assertThat(StorageKeys.fileNameOf("courses/c/materials/u/tema-1.pdf"))
                .isEqualTo("tema-1.pdf");
    }
}
