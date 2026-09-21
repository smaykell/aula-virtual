-- V9__create_enrollments.sql
--
-- La matricula es la union de un estudiante con un curso, y el curso decide
-- como nace: con enrollment_policy AUTOMATIC entra ACTIVE, con ON_REQUEST
-- entra PENDING y espera al docente.
--
-- La unicidad es (course_id, student_id) sin el estado: un estudiante tiene
-- una sola fila por curso y esa fila se reutiliza si vuelve a pedir entrar
-- despues de un rechazo o una baja, para que su historial no se multiplique.

ALTER TABLE courses ADD COLUMN enrollment_policy VARCHAR(20) NOT NULL DEFAULT 'ON_REQUEST';
ALTER TABLE courses ADD CONSTRAINT ck_courses_enrollment_policy CHECK (
    enrollment_policy IN ('AUTOMATIC', 'ON_REQUEST'));

CREATE TABLE enrollments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_enrollments_course_student UNIQUE (course_id, student_id),
    CONSTRAINT fk_enrollments_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_enrollments_student_id FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT ck_enrollments_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX idx_enrollments_course_id ON enrollments (course_id, status);
CREATE INDEX idx_enrollments_student_id ON enrollments (student_id, status);
