package io.github.smaykell.aulavirtual.assignment.dto;

public final class AssignmentConstraints {

    public static final int TITLE_MAX = 150;
    public static final int INSTRUCTIONS_MAX = 4000;
    public static final int TEXT_MAX = 10000;
    public static final int STORAGE_KEY_MAX = 255;
    public static final String TITLE_REQUIRED = "el título es obligatorio";
    public static final String TITLE_TOO_LONG = "no puede superar los 150 caracteres";
    public static final String INSTRUCTIONS_TOO_LONG = "no puede superar los 4000 caracteres";
    public static final String TEXT_TOO_LONG = "no puede superar los 10000 caracteres";
    public static final String STORAGE_KEY_TOO_LONG = "no puede superar los 255 caracteres";
    public static final String DUE_AT_REQUIRED = "la fecha límite es obligatoria";
    public static final String MAX_SCORE_REQUIRED = "el puntaje máximo es obligatorio";
    public static final String MAX_SCORE_POSITIVE = "debe ser mayor que cero";
    public static final String LATE_REQUIRED = "hay que decidir si se admiten entregas tardías";
    public static final String SCORE_REQUIRED = "la nota es obligatoria";
    public static final String SCORE_NOT_NEGATIVE = "no puede ser negativa";
    public static final String STUDENTS_REQUIRED = "indica a qué estudiantes devolver la nota";

    private AssignmentConstraints() {
    }
}
