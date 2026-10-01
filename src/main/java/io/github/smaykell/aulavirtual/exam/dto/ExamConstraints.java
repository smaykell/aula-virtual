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
    public static final int TITLE_MAX = 150;
    public static final int INSTRUCTIONS_MAX = 4000;
    public static final int TIME_LIMIT_MAX = 1440;
    public static final int ATTEMPTS_MAX = 2;
    public static final int QUESTIONS_MAX = 100;
    public static final String TITLE_REQUIRED = "el título es obligatorio";
    public static final String TITLE_TOO_LONG = "no puede superar los 150 caracteres";
    public static final String INSTRUCTIONS_TOO_LONG = "no puede superar los 4000 caracteres";
    public static final String OPENS_AT_REQUIRED = "la hora de apertura es obligatoria";
    public static final String CLOSES_AT_REQUIRED = "la hora de cierre es obligatoria";
    public static final String TIME_LIMIT_POSITIVE = "debe ser mayor que cero";
    public static final String TIME_LIMIT_TOO_LONG = "no puede superar las 24 horas";
    public static final String ATTEMPTS_REQUIRED = "indica cuántos intentos se permiten";
    public static final String ATTEMPTS_RANGE = "se permiten uno o dos intentos";
    public static final String SHUFFLE_QUESTIONS_REQUIRED =
            "indica si las preguntas salen en orden aleatorio";
    public static final String SHUFFLE_OPTIONS_REQUIRED =
            "indica si las opciones salen en orden aleatorio";
    public static final String SHOWS_ANSWERS_REQUIRED =
            "indica si se muestran las respuestas correctas al devolver la nota";
    public static final String QUESTIONS_REQUIRED = "indica las preguntas del examen";
    public static final String QUESTIONS_TOO_MANY = "un examen admite como mucho 100 preguntas";
    public static final String QUESTION_REQUIRED = "indica la pregunta";
    public static final String POINTS_REQUIRED = "el puntaje de la pregunta es obligatorio";
    public static final String POINTS_POSITIVE = "debe ser mayor que cero";

    private ExamConstraints() {
    }
}
