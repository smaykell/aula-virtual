-- V22__create_announcements.sql
--
-- El tablón del curso: lo que el staff escribe a sus estudiantes.

CREATE TABLE announcements (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    author_person_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_announcements_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_announcements_author_person_id
        FOREIGN KEY (author_person_id) REFERENCES persons (id)
);

CREATE INDEX idx_announcements_course_id_created_at
    ON announcements (course_id, created_at DESC);
