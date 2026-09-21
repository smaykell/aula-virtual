CREATE TABLE administrators (
    id UUID PRIMARY KEY,
    person_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_administrators_person_id UNIQUE (person_id),
    CONSTRAINT fk_administrators_person_id FOREIGN KEY (person_id) REFERENCES persons (id),
    CONSTRAINT ck_administrators_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN'))
);

CREATE INDEX idx_administrators_role ON administrators (role);
CREATE INDEX idx_administrators_active ON administrators (active);
