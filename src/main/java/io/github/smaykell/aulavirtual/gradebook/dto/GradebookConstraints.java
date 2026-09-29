package io.github.smaykell.aulavirtual.gradebook.dto;

public final class GradebookConstraints {

    public static final int CATEGORY_NAME_MAX = 80;
    public static final int CATEGORIES_MAX = 20;
    public static final String SCALE_MAX = "20";
    public static final String WEIGHT_MAX = "100";
    public static final String METHOD_REQUIRED = "elige cómo se calcula la nota final";
    public static final String PASSING_SCORE_REQUIRED = "la nota aprobatoria es obligatoria";
    public static final String PASSING_SCORE_RANGE = "debe ser mayor que 0 y como mucho 20";
    public static final String CATEGORIES_REQUIRED = "envía la lista de categorías, aunque esté vacía";
    public static final String CATEGORIES_TOO_MANY = "no puede haber más de 20 categorías";
    public static final String CATEGORY_NAME_REQUIRED = "el nombre de la categoría es obligatorio";
    public static final String CATEGORY_NAME_TOO_LONG = "no puede superar los 80 caracteres";
    public static final String WEIGHT_REQUIRED = "el peso es obligatorio";
    public static final String WEIGHT_RANGE = "debe estar entre 0 y 100";

    private GradebookConstraints() {
    }
}
