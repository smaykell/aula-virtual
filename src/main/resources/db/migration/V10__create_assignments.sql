-- V10__create_assignments.sql
--
-- La tarea cuelga de una unidad y la entrega es de un estudiante: una sola
-- por tarea, que se reemplaza mientras no este calificada.
--
-- La calificacion es una tabla aparte y polimorfica (source_type/source_id)
-- para que el examen se enchufe sin tocarla y el consolidado de notas de un
-- curso salga de una sola consulta. course_id va desnormalizado aqui por eso
-- mismo: es el filtro del consolidado.

CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    instructions VARCHAR(4000),
    due_at TIMESTAMPTZ NOT NULL,
    max_score NUMERIC(5, 2) NOT NULL,
    allows_late BOOLEAN NOT NULL DEFAULT FALSE,
    attachment_key VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_assignments_unit_id FOREIGN KEY (unit_id) REFERENCES units (id),
    CONSTRAINT ck_assignments_max_score CHECK (max_score > 0)
);

CREATE INDEX idx_assignments_unit_id ON assignments (unit_id, due_at);

CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    storage_key VARCHAR(255),
    text VARCHAR(10000),
    submitted_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_submissions_assignment_student UNIQUE (assignment_id, student_id),
    CONSTRAINT fk_submissions_assignment_id FOREIGN KEY (assignment_id)
        REFERENCES assignments (id),
    CONSTRAINT fk_submissions_student_id FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT ck_submissions_status CHECK (status IN ('SUBMITTED', 'LATE', 'GRADED')),
    CONSTRAINT ck_submissions_content CHECK (storage_key IS NOT NULL OR text IS NOT NULL)
);

CREATE INDEX idx_submissions_assignment_id ON submissions (assignment_id, status);
CREATE INDEX idx_submissions_student_id ON submissions (student_id);

CREATE TABLE grades (
    id UUID PRIMARY KEY,
    source_type VARCHAR(20) NOT NULL,
    source_id UUID NOT NULL,
    student_id UUID NOT NULL,
    course_id UUID NOT NULL,
    score NUMERIC(5, 2) NOT NULL,
    feedback VARCHAR(4000),
    graded_by UUID,
    graded_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_grades_source UNIQUE (source_type, source_id),
    CONSTRAINT fk_grades_student_id FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_grades_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_grades_graded_by FOREIGN KEY (graded_by) REFERENCES persons (id),
    CONSTRAINT ck_grades_source_type CHECK (source_type IN ('ASSIGNMENT', 'EXAM')),
    CONSTRAINT ck_grades_score CHECK (score >= 0)
);

CREATE INDEX idx_grades_course_student ON grades (course_id, student_id);
