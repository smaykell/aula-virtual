-- V24__create_exams.sql
--
-- El examen cuelga de una unidad, como la tarea, y guarda su curso por la
-- misma razon. Sus preguntas salen del banco del curso con un puntaje propio
-- en cada examen; max_score es su suma, recalculada al cambiar la lista.

CREATE TABLE exams (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL,
    course_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    instructions VARCHAR(4000),
    opens_at TIMESTAMPTZ NOT NULL,
    closes_at TIMESTAMPTZ NOT NULL,
    time_limit_minutes INTEGER,
    max_attempts INTEGER NOT NULL DEFAULT 1,
    shuffle_questions BOOLEAN NOT NULL DEFAULT FALSE,
    shuffle_options BOOLEAN NOT NULL DEFAULT FALSE,
    shows_answers BOOLEAN NOT NULL DEFAULT FALSE,
    category_id UUID,
    max_score NUMERIC(5, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_exams_unit_id FOREIGN KEY (unit_id) REFERENCES units (id),
    CONSTRAINT fk_exams_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_exams_category_id FOREIGN KEY (category_id)
        REFERENCES grade_categories (id) ON DELETE SET NULL,
    CONSTRAINT ck_exams_window CHECK (closes_at > opens_at),
    CONSTRAINT ck_exams_time_limit CHECK (time_limit_minutes IS NULL OR time_limit_minutes > 0),
    CONSTRAINT ck_exams_max_attempts CHECK (max_attempts IN (1, 2)),
    CONSTRAINT ck_exams_max_score CHECK (max_score >= 0)
);

CREATE INDEX idx_exams_unit_id ON exams (unit_id, opens_at);
CREATE INDEX idx_exams_course_id ON exams (course_id, closes_at);

CREATE TABLE exam_questions (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL,
    question_id UUID NOT NULL,
    position INTEGER NOT NULL,
    points NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_exam_questions_exam_id FOREIGN KEY (exam_id)
        REFERENCES exams (id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_questions_question_id FOREIGN KEY (question_id)
        REFERENCES questions (id),
    CONSTRAINT uk_exam_questions_exam_question UNIQUE (exam_id, question_id),
    CONSTRAINT uk_exam_questions_exam_position UNIQUE (exam_id, position),
    CONSTRAINT ck_exam_questions_points CHECK (points > 0)
);

CREATE INDEX idx_exam_questions_question_id ON exam_questions (question_id);
