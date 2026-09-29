-- V17__create_grading_schemes.sql
--
-- Cada curso decide como se calcula su nota final: por categorias con peso o
-- por total de puntos. Un curso sin fila usa el esquema por defecto (total de
-- puntos, aprobatoria 13), asi que no hace falta sembrar nada.
--
-- El nombre de categoria es unico por curso con restriccion diferida: el
-- esquema se reemplaza entero en una transaccion y Hibernate inserta antes de
-- borrar, asi que renombrar o recrear una categoria pasa por un estado
-- intermedio repetido. Borrar una categoria deja sus tareas sin categoria.

CREATE TABLE grading_schemes (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    method VARCHAR(20) NOT NULL,
    passing_score NUMERIC(4, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_grading_schemes_course_id UNIQUE (course_id),
    CONSTRAINT fk_grading_schemes_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT ck_grading_schemes_method CHECK (method IN ('WEIGHTED', 'TOTAL_POINTS')),
    CONSTRAINT ck_grading_schemes_passing_score
        CHECK (passing_score > 0 AND passing_score <= 20)
);

CREATE TABLE grade_categories (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    weight NUMERIC(5, 2) NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_grade_categories_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uk_grade_categories_course_name UNIQUE (course_id, name)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_grade_categories_weight CHECK (weight >= 0 AND weight <= 100)
);

CREATE INDEX idx_grade_categories_course_id ON grade_categories (course_id, position);

ALTER TABLE assignments ADD COLUMN category_id UUID;
ALTER TABLE assignments
    ADD CONSTRAINT fk_assignments_category_id FOREIGN KEY (category_id)
        REFERENCES grade_categories (id) ON DELETE SET NULL;
