-- V23__create_questions.sql
--
-- El banco de preguntas del curso, del que se arman los examenes. Verdadero o
-- falso se guarda como opcion unica con sus dos opciones, para que corregir
-- sea siempre comparar lo marcado con lo correcto.

CREATE TABLE questions (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    statement VARCHAR(4000) NOT NULL,
    model_answer VARCHAR(4000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_questions_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT ck_questions_type
        CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER'))
);

CREATE INDEX idx_questions_course_id ON questions (course_id, created_at);

CREATE TABLE question_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL,
    position INTEGER NOT NULL,
    text VARCHAR(1000) NOT NULL,
    correct BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_question_options_question_id FOREIGN KEY (question_id)
        REFERENCES questions (id) ON DELETE CASCADE,
    CONSTRAINT uk_question_options_question_position UNIQUE (question_id, position)
);
