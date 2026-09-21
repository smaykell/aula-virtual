CREATE TABLE students (
    id UUID PRIMARY KEY,
    person_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_students_person_id UNIQUE (person_id),
    CONSTRAINT fk_students_person_id FOREIGN KEY (person_id) REFERENCES persons (id)
);

CREATE INDEX idx_students_active ON students (active);
