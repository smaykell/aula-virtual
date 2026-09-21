package io.github.smaykell.aulavirtual.modules.person.dto;

public final class PersonConstraints {

    public static final int NAME_MAX = 100;
    public static final int DOCUMENT_NUMBER_MAX = 20;
    public static final String NAME_TOO_LONG = "no puede superar los 100 caracteres";
    public static final String FIRST_NAME_REQUIRED = "los nombres son obligatorios";
    public static final String LAST_NAME_REQUIRED = "los apellidos son obligatorios";
    public static final String BIRTH_DATE_REQUIRED = "la fecha de nacimiento es obligatoria";
    public static final String BIRTH_DATE_IN_THE_PAST = "debe ser anterior a hoy";
    public static final String SEX_REQUIRED = "el sexo es obligatorio";
    public static final String DOCUMENT_TYPE_REQUIRED = "el tipo de documento es obligatorio";
    public static final String DOCUMENT_NUMBER_REQUIRED = "el número de documento es obligatorio";
    public static final String DOCUMENT_NUMBER_TOO_LONG =
            "no puede superar los 20 caracteres";

    private PersonConstraints() {
    }
}
