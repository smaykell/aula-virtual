package io.github.smaykell.aulavirtual.person;

import java.util.regex.Pattern;

public enum DocumentType {

    DNI("^[0-9]{8}$"),
    FOREIGNER_CARD("^[0-9]{9,12}$"),
    PASSPORT("^[A-Za-z0-9]{6,12}$");

    private final Pattern pattern;

    DocumentType(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public boolean accepts(String documentNumber) {
        return pattern.matcher(documentNumber).matches();
    }
}
