package io.github.smaykell.aulavirtual.course;

public enum MaterialType {

    PDF,
    VIDEO,
    PPT,
    DOC,
    LINK;

    public boolean isLink() {
        return this == LINK;
    }
}
