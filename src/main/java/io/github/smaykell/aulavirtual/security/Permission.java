package io.github.smaykell.aulavirtual.security;

public enum Permission {

    ADMINISTRATORS_READ(Name.ADMINISTRATORS_READ),
    ADMINISTRATORS_CREATE(Name.ADMINISTRATORS_CREATE),
    ADMINISTRATORS_UPDATE(Name.ADMINISTRATORS_UPDATE),
    TEACHERS_READ(Name.TEACHERS_READ),
    TEACHERS_CREATE(Name.TEACHERS_CREATE),
    TEACHERS_UPDATE(Name.TEACHERS_UPDATE),
    COURSES_READ(Name.COURSES_READ),
    COURSES_CREATE(Name.COURSES_CREATE),
    COURSES_UPDATE(Name.COURSES_UPDATE);

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }

    public static final class Name {

        public static final String ADMINISTRATORS_READ = "administrators:read";
        public static final String ADMINISTRATORS_CREATE = "administrators:create";
        public static final String ADMINISTRATORS_UPDATE = "administrators:update";
        public static final String TEACHERS_READ = "teachers:read";
        public static final String TEACHERS_CREATE = "teachers:create";
        public static final String TEACHERS_UPDATE = "teachers:update";
        public static final String COURSES_READ = "courses:read";
        public static final String COURSES_CREATE = "courses:create";
        public static final String COURSES_UPDATE = "courses:update";

        private Name() {
        }
    }
}
