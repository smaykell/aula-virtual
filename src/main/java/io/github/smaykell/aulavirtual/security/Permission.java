package io.github.smaykell.aulavirtual.security;

public enum Permission {

    USERS_READ(Name.USERS_READ),
    USERS_CREATE(Name.USERS_CREATE),
    USERS_UPDATE(Name.USERS_UPDATE),
    TEACHERS_READ(Name.TEACHERS_READ),
    TEACHERS_CREATE(Name.TEACHERS_CREATE),
    TEACHERS_UPDATE(Name.TEACHERS_UPDATE);

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }

    public static final class Name {

        public static final String USERS_READ = "users:read";
        public static final String USERS_CREATE = "users:create";
        public static final String USERS_UPDATE = "users:update";
        public static final String TEACHERS_READ = "teachers:read";
        public static final String TEACHERS_CREATE = "teachers:create";
        public static final String TEACHERS_UPDATE = "teachers:update";

        private Name() {
        }
    }
}
