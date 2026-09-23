package io.github.smaykell.aulavirtual.course.dto;

public final class CourseConstraints {

    public static final int NAME_MAX = 150;
    public static final int DESCRIPTION_MAX = 4000;
    public static final int STORAGE_KEY_MAX = 255;
    public static final int EXTERNAL_URL_MAX = 2048;
    public static final int FILE_NAME_MAX = 255;
    public static final int CONTENT_TYPE_MAX = 255;
    public static final String NAME_REQUIRED = "el nombre es obligatorio";
    public static final String NAME_TOO_LONG = "no puede superar los 150 caracteres";
    public static final String DESCRIPTION_TOO_LONG = "no puede superar los 4000 caracteres";
    public static final String TITLE_REQUIRED = "el título es obligatorio";
    public static final String START_DATE_REQUIRED = "la fecha de inicio es obligatoria";
    public static final String END_DATE_REQUIRED = "la fecha de fin es obligatoria";
    public static final String TEACHER_REQUIRED = "el docente titular es obligatorio";
    public static final String TYPE_REQUIRED = "el tipo de material es obligatorio";
    public static final String VISIBILITY_REQUIRED = "la visibilidad es obligatoria";
    public static final String STORAGE_KEY_TOO_LONG = "no puede superar los 255 caracteres";
    public static final String EXTERNAL_URL_TOO_LONG = "no puede superar los 2048 caracteres";
    public static final String FILE_NAME_REQUIRED = "el nombre del archivo es obligatorio";
    public static final String FILE_NAME_TOO_LONG = "no puede superar los 255 caracteres";
    public static final String CONTENT_TYPE_REQUIRED = "el tipo de archivo es obligatorio";
    public static final String CONTENT_TYPE_TOO_LONG = "no puede superar los 255 caracteres";
    public static final String SIZE_REQUIRED = "el tamaño del archivo es obligatorio";
    public static final String SIZE_POSITIVE = "el archivo no puede estar vacío";
    public static final String POLICY_REQUIRED =
            "hay que decidir si el curso acepta las inscripciones o las revisa";
    public static final String INVITATION_CODE_REQUIRED = "el código de invitación es obligatorio";
    public static final String UNITS_REQUIRED = "el orden de las unidades es obligatorio";

    private CourseConstraints() {
    }
}
