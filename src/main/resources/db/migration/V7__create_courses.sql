-- V7__create_courses.sql
--
-- El curso es el agregado: sus unidades y el material de cada unidad no viven
-- fuera de el. El docente titular apunta al perfil (teachers), no a la persona:
-- el alcance sobre un curso se decide por perfil docente.
--
-- La unicidad de (course_id, position) es DEFERRABLE porque reordenar unidades
-- pasa por estados intermedios con posiciones repetidas dentro de la misma
-- transaccion; al hacer commit el orden ya es 1..n sin huecos.

CREATE TABLE courses (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(4000),
    teacher_id UUID NOT NULL,
    invitation_code VARCHAR(12) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_courses_invitation_code UNIQUE (invitation_code),
    CONSTRAINT fk_courses_teacher_id FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT ck_courses_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_courses_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_courses_teacher_id ON courses (teacher_id);
CREATE INDEX idx_courses_status ON courses (status);

CREATE TABLE units (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_units_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uk_units_course_position UNIQUE (course_id, position)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_units_position CHECK (position > 0)
);

CREATE INDEX idx_units_course_id ON units (course_id, position);

CREATE TABLE materials (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    type VARCHAR(20) NOT NULL,
    storage_key VARCHAR(255),
    external_url VARCHAR(2048),
    published_at TIMESTAMPTZ NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_materials_unit_id FOREIGN KEY (unit_id) REFERENCES units (id),
    CONSTRAINT ck_materials_type CHECK (type IN ('PDF', 'VIDEO', 'PPT', 'DOC', 'LINK')),
    CONSTRAINT ck_materials_source CHECK (
        (type = 'LINK' AND external_url IS NOT NULL AND storage_key IS NULL)
        OR (type <> 'LINK' AND storage_key IS NOT NULL AND external_url IS NULL))
);

CREATE INDEX idx_materials_unit_id ON materials (unit_id, published_at);
