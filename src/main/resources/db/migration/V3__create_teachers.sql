CREATE TABLE teachers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    sex VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_teachers_user_id UNIQUE (user_id),
    CONSTRAINT fk_teachers_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_teachers_sex CHECK (sex IN ('MALE', 'FEMALE'))
);

CREATE INDEX idx_teachers_active ON teachers (active);
