-- V25__create_exam_attempts.sql
--
-- Un intento por fila, numerado por estudiante; el numero unico es lo que
-- impide que dos peticiones simultaneas abran dos primeros intentos. El plazo
-- lo fija el servidor al empezar y la semilla fija el orden aleatorio, para
-- que recargar la pagina no baraje otra vez. Solo se guardan las preguntas
-- respondidas: la que falta vale cero.

CREATE TABLE exam_attempts (
    id UUID PRIMARY KEY,
    exam_id UUID NOT NULL,
    student_id UUID NOT NULL,
    number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    deadline TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    seed BIGINT NOT NULL,
    score NUMERIC(5, 2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_exam_attempts_exam_id FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT fk_exam_attempts_student_id FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT uk_exam_attempts_exam_student_number UNIQUE (exam_id, student_id, number),
    CONSTRAINT ck_exam_attempts_number CHECK (number IN (1, 2)),
    CONSTRAINT ck_exam_attempts_status
        CHECK (status IN ('IN_PROGRESS', 'PENDING_REVIEW', 'GRADED')),
    CONSTRAINT ck_exam_attempts_score CHECK (score IS NULL OR score >= 0)
);

CREATE INDEX idx_exam_attempts_open ON exam_attempts (deadline) WHERE status = 'IN_PROGRESS';

CREATE TABLE attempt_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    question_id UUID NOT NULL,
    selected_option_ids UUID[],
    text VARCHAR(4000),
    points NUMERIC(5, 2),
    feedback VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_attempt_answers_attempt_id FOREIGN KEY (attempt_id)
        REFERENCES exam_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_answers_question_id FOREIGN KEY (question_id)
        REFERENCES questions (id),
    CONSTRAINT uk_attempt_answers_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT ck_attempt_answers_points CHECK (points IS NULL OR points >= 0)
);
