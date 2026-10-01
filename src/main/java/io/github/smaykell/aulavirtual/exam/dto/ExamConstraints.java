package io.github.smaykell.aulavirtual.exam.dto;

public final class ExamConstraints {

    public static final int STATEMENT_MAX = 4000;
    public static final int MODEL_ANSWER_MAX = 4000;
    public static final int OPTION_TEXT_MAX = 1000;
    public static final int CHOICES_MIN = 2;
    public static final int CHOICES_MAX = 10;
    public static final String TYPE_REQUIRED = "el tipo de pregunta es obligatorio";
    public static final String STATEMENT_REQUIRED = "el enunciado es obligatorio";
    public static final String STATEMENT_TOO_LONG = "no puede superar los 4000 caracteres";
    public static final String MODEL_ANSWER_TOO_LONG = "no puede superar los 4000 caracteres";
    public static final String OPTION_TEXT_REQUIRED = "el texto de la opción es obligatorio";
    public static final String OPTION_TEXT_TOO_LONG = "no puede superar los 1000 caracteres";
    public static final String OPTION_CORRECT_REQUIRED = "indica si la opción es correcta";
    public static final String CHOICES_TOO_MANY = "se admiten como mucho 10 opciones";

    private ExamConstraints() {
    }
}
