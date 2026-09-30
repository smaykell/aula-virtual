package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.gradebook.dto.CategoryAverage;
import io.github.smaykell.aulavirtual.gradebook.dto.FinalGrade;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeCategoryResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookRow;
import io.github.smaykell.aulavirtual.person.DocumentType;
import io.github.smaykell.aulavirtual.student.dto.StudentDocument;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class GradebookCsv {

    private static final String BYTE_ORDER_MARK = "﻿";
    private static final String SEPARATOR = ",";
    private static final String LINE_END = "\r\n";
    private static final String QUOTE = "\"";
    private static final Pattern NEEDS_QUOTES = Pattern.compile("[,\"\r\n]");
    private static final Pattern EXCEL_FORMULA_START = Pattern.compile("^[=+\\-@\t\r]");
    private static final String AS_TEXT = "'";

    private final GradebookResponse gradebook;
    private final Map<UUID, StudentDocument> documents;

    GradebookCsv(GradebookResponse gradebook, Map<UUID, StudentDocument> documents) {
        this.gradebook = gradebook;
        this.documents = documents;
    }

    byte[] bytes() {
        StringBuilder csv = new StringBuilder(BYTE_ORDER_MARK);
        append(csv, header());
        List<GradebookRow> rows = gradebook.rows();
        for (int index = 0; index < rows.size(); index++) {
            append(csv, cellsOf(index + 1, rows.get(index)));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<String> header() {
        List<String> header = new ArrayList<>(List.of(
                "N°", "Tipo de documento", "Número de documento", "Apellidos", "Nombres"));
        gradebook.items().forEach(item -> header.add(titleOf(item)));
        categories().forEach(category -> header.add(titleOf(category)));
        header.addAll(List.of("Nota final", "Nota de acta", "Condición"));
        return header;
    }

    private List<String> cellsOf(int number, GradebookRow row) {
        StudentDocument document = documents.get(row.student().id());
        List<String> cells = new ArrayList<>(List.of(String.valueOf(number),
                labelOf(document.type()), document.number(),
                row.student().lastName(), row.student().firstName()));

        Map<UUID, BigDecimal> scores = row.grades().stream()
                .collect(Collectors.toMap(GradeResponse::sourceId, GradeResponse::score));
        gradebook.items().forEach(item -> cells.add(plain(scores.get(item.sourceId()))));

        Map<UUID, BigDecimal> averages = row.categories().stream()
                .collect(Collectors.toMap(CategoryAverage::categoryId, CategoryAverage::score));
        categories().forEach(category -> cells.add(fixed(averages.get(category.id()))));

        cells.addAll(finalCells(row.finalGrade()));
        return cells;
    }

    private static List<String> finalCells(FinalGrade finalGrade) {
        if (finalGrade == null) {
            return List.of("", "", "Sin notas");
        }
        return List.of(fixed(finalGrade.score()), String.valueOf(finalGrade.roundedScore()),
                finalGrade.passed() ? "Aprobado" : "Desaprobado");
    }

    private List<GradeCategoryResponse> categories() {
        return gradebook.scheme().categories();
    }

    private String titleOf(GradeCategoryResponse category) {
        return gradebook.scheme().method() == GradingMethod.WEIGHTED
                ? category.name() + " (" + plain(category.weight()) + "%)"
                : category.name();
    }

    private static String titleOf(GradeItem item) {
        return item.title() + " (/" + plain(item.maxScore()) + ")";
    }

    private static String labelOf(DocumentType type) {
        return switch (type) {
            case DNI -> "DNI";
            case FOREIGNER_CARD -> "Carné de extranjería";
            case PASSPORT -> "Pasaporte";
        };
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String fixed(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static void append(StringBuilder csv, List<String> cells) {
        csv.append(cells.stream().map(GradebookCsv::escaped)
                .collect(Collectors.joining(SEPARATOR))).append(LINE_END);
    }

    private static String escaped(String value) {
        String text = EXCEL_FORMULA_START.matcher(value).find() ? AS_TEXT + value : value;
        return NEEDS_QUOTES.matcher(text).find()
                ? QUOTE + text.replace(QUOTE, QUOTE + QUOTE) + QUOTE
                : text;
    }
}
