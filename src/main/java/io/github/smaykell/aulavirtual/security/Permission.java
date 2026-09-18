package io.github.smaykell.aulavirtual.security;

public enum Permission {

    USERS_READ(Name.USERS_READ),
    USERS_CREATE(Name.USERS_CREATE);

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

        private Name() {
        }
    }
}
