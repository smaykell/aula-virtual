package io.github.smaykell.aulavirtual.user.dto;

public final class CredentialConstraints {

    public static final String USERNAME_PATTERN = "^[A-Za-z0-9._@+-]{3,160}$";
    public static final String USERNAME_REQUIRED = "el usuario es obligatorio";
    public static final String USERNAME_SHAPE =
            "debe tener entre 3 y 160 caracteres, sin espacios ni símbolos distintos de . _ - + @";
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 72;
    public static final String PASSWORD_REQUIRED = "la contraseña es obligatoria";
    public static final String PASSWORD_SHAPE = "debe tener entre 8 y 72 caracteres";

    private CredentialConstraints() {
    }
}
