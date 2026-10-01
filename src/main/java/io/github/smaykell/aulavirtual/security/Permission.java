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
    COURSES_UPDATE(Name.COURSES_UPDATE),
    STUDENTS_READ(Name.STUDENTS_READ),
    STUDENTS_CREATE(Name.STUDENTS_CREATE),
    STUDENTS_UPDATE(Name.STUDENTS_UPDATE),
    ENROLLMENTS_READ(Name.ENROLLMENTS_READ),
    ENROLLMENTS_CREATE(Name.ENROLLMENTS_CREATE),
    ENROLLMENTS_UPDATE(Name.ENROLLMENTS_UPDATE),
    ASSIGNMENTS_READ(Name.ASSIGNMENTS_READ),
    ASSIGNMENTS_CREATE(Name.ASSIGNMENTS_CREATE),
    ASSIGNMENTS_UPDATE(Name.ASSIGNMENTS_UPDATE),
    SUBMISSIONS_CREATE(Name.SUBMISSIONS_CREATE),
    EXAMS_READ(Name.EXAMS_READ),
    EXAMS_CREATE(Name.EXAMS_CREATE),
    EXAMS_UPDATE(Name.EXAMS_UPDATE),
    ATTEMPTS_CREATE(Name.ATTEMPTS_CREATE),
    QUESTIONS_READ(Name.QUESTIONS_READ),
    QUESTIONS_UPDATE(Name.QUESTIONS_UPDATE),
    SETTINGS_READ(Name.SETTINGS_READ),
    SETTINGS_UPDATE(Name.SETTINGS_UPDATE);

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
        public static final String STUDENTS_READ = "students:read";
        public static final String STUDENTS_CREATE = "students:create";
        public static final String STUDENTS_UPDATE = "students:update";
        public static final String ENROLLMENTS_READ = "enrollments:read";
        public static final String ENROLLMENTS_CREATE = "enrollments:create";
        public static final String ENROLLMENTS_UPDATE = "enrollments:update";
        public static final String ASSIGNMENTS_READ = "assignments:read";
        public static final String ASSIGNMENTS_CREATE = "assignments:create";
        public static final String ASSIGNMENTS_UPDATE = "assignments:update";
        public static final String SUBMISSIONS_CREATE = "submissions:create";
        public static final String EXAMS_READ = "exams:read";
        public static final String EXAMS_CREATE = "exams:create";
        public static final String EXAMS_UPDATE = "exams:update";
        public static final String ATTEMPTS_CREATE = "attempts:create";
        public static final String QUESTIONS_READ = "questions:read";
        public static final String QUESTIONS_UPDATE = "questions:update";
        public static final String SETTINGS_READ = "settings:read";
        public static final String SETTINGS_UPDATE = "settings:update";

        private Name() {
        }
    }
}
