package io.github.smaykell.aulavirtual.gradebook;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.smaykell.aulavirtual.gradebook.dto.CategoryAverage;
import io.github.smaykell.aulavirtual.gradebook.dto.FinalGrade;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeCategoryResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookRow;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.student.dto.StudentDocument;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradebookCsvTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LUIS = UUID.randomUUID();
    private static final GradeCategoryResponse PRACTICE = new GradeCategoryResponse(
            UUID.randomUUID(), "Prácticas", new BigDecimal("60.00"), 1);
    private static final GradeCategoryResponse EXAMS = new GradeCategoryResponse(
            UUID.randomUUID(), "Exámenes", new BigDecimal("40.00"), 2);
    private static final GradeItem FIRST_TASK = item("Práctica 1", PRACTICE);
    private static final GradeItem SECOND_TASK = item("Práctica 2", PRACTICE);
    private static final Map<UUID, StudentDocument> DOCUMENTS = Map.of(
            ANA, new StudentDocument(DocumentType.DNI, "45678912"),
            LUIS, new StudentDocument(DocumentType.FOREIGNER_CARD, "001234567"));

    @Test
    void starts_with_a_byte_order_mark_so_excel_reads_the_accents() {
        byte[] csv = csvOf(weighted(), List.of());

        assertThat(new String(csv, StandardCharsets.UTF_8)).startsWith("﻿");
    }

    @Test
    void the_header_lists_the_student_the_items_the_weighted_categories_and_the_final_grade() {
        assertThat(linesOf(weighted(), List.of()).getFirst()).isEqualTo(
                "N°,Tipo de documento,Número de documento,Apellidos,Nombres,"
                        + "Práctica 1 (/20),Práctica 2 (/20),Prácticas (60%),Exámenes (40%),"
                        + "Nota final,Nota de acta,Condición");
    }

    @Test
    void under_total_points_the_categories_carry_no_weight() {
        GradingSchemeResponse scheme = new GradingSchemeResponse(GradingMethod.TOTAL_POINTS,
                new BigDecimal("13.00"), List.of(PRACTICE));

        assertThat(linesOf(scheme, List.of()).getFirst()).contains(",Prácticas,");
    }

    @Test
    void each_row_numbers_the_student_and_leaves_blank_what_was_not_graded() {
        GradebookRow ana = new GradebookRow(student(ANA, "Ana", "Quispe Rojas"),
                List.of(grade(ANA, FIRST_TASK, "16.00")),
                List.of(new CategoryAverage(PRACTICE.id(), new BigDecimal("16.00"))),
                FinalGrade.of(new BigDecimal("16"), new BigDecimal("13.00")));

        assertThat(linesOf(weighted(), List.of(ana)).get(1))
                .isEqualTo("1,DNI,45678912,Quispe Rojas,Ana,16,,16.00,,16.00,16,Aprobado");
    }

    @Test
    void a_student_without_grades_says_so_instead_of_failing() {
        GradebookRow luis = new GradebookRow(student(LUIS, "Luis", "Álvarez Ruiz"),
                List.of(), List.of(), null);

        assertThat(linesOf(weighted(), List.of(luis)).get(1))
                .isEqualTo("1,Carné de extranjería,001234567,Álvarez Ruiz,Luis,,,,,,,Sin notas");
    }

    @Test
    void the_record_grade_is_rounded_from_the_two_decimals_and_decides_the_condition() {
        GradebookRow ana = new GradebookRow(student(ANA, "Ana", "Quispe Rojas"),
                List.of(), List.of(),
                FinalGrade.of(new BigDecimal("12.495"), new BigDecimal("13.00")));

        assertThat(linesOf(weighted(), List.of(ana)).get(1))
                .endsWith(",12.50,13,Aprobado");
    }

    @Test
    void a_comma_or_a_quote_inside_a_title_is_quoted() {
        GradeItem quoted = new GradeItem(GradeSource.ASSIGNMENT, UUID.randomUUID(),
                "Caso \"A\", parte 1", null, new BigDecimal("20.00"), null);
        GradebookResponse gradebook = new GradebookResponse(weighted(), List.of(quoted),
                List.of());

        assertThat(linesOf(gradebook).getFirst())
                .contains(",\"Caso \"\"A\"\", parte 1 (/20)\",");
    }

    @Test
    void a_name_that_looks_like_a_formula_is_kept_as_text() {
        GradebookRow row = new GradebookRow(student(ANA, "=HYPERLINK(\"x\")", "+Quispe"),
                List.of(), List.of(), null);

        assertThat(linesOf(weighted(), List.of(row)).get(1))
                .contains(",'+Quispe,\"'=HYPERLINK(\"\"x\"\")\",");
    }

    private static List<String> linesOf(GradingSchemeResponse scheme, List<GradebookRow> rows) {
        return linesOf(new GradebookResponse(scheme, List.of(FIRST_TASK, SECOND_TASK), rows));
    }

    private static List<String> linesOf(GradebookResponse gradebook) {
        String csv = new String(new GradebookCsv(gradebook, DOCUMENTS).bytes(),
                StandardCharsets.UTF_8);
        return List.of(csv.substring(1).split("\r\n"));
    }

    private static byte[] csvOf(GradingSchemeResponse scheme, List<GradebookRow> rows) {
        return new GradebookCsv(new GradebookResponse(scheme, List.of(), rows), DOCUMENTS)
                .bytes();
    }

    private static GradingSchemeResponse weighted() {
        return new GradingSchemeResponse(GradingMethod.WEIGHTED, new BigDecimal("13.00"),
                List.of(PRACTICE, EXAMS));
    }

    private static GradeItem item(String title, GradeCategoryResponse category) {
        return new GradeItem(GradeSource.ASSIGNMENT, UUID.randomUUID(), title, category.id(),
                new BigDecimal("20.00"), null);
    }

    private static GradeResponse grade(UUID studentId, GradeItem item, String score) {
        return new GradeResponse(UUID.randomUUID(), item.sourceType(), item.sourceId(),
                studentId, UUID.randomUUID(), new BigDecimal(score), item.maxScore(), null,
                null, null);
    }

    private static StudentSummary student(UUID id, String firstName, String lastName) {
        return new StudentSummary(id, firstName, lastName, "Hospital Regional", true);
    }
}
