-- V4__create_persons.sql
--
-- La persona es la identidad y el documento es su clave natural: es lo unico
-- que permite reconocer que el docente que se esta registrando ya existe en la
-- base como estudiante. Los perfiles (teachers, administrators, students)
-- apuntan aqui y aportan solo lo suyo.

CREATE TABLE persons (
    id UUID PRIMARY KEY,
    document_type VARCHAR(20) NOT NULL,
    document_number VARCHAR(20) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    sex VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_persons_document UNIQUE (document_type, document_number),
    CONSTRAINT ck_persons_document_type CHECK (
        document_type IN ('DNI', 'FOREIGNER_CARD', 'PASSPORT')),
    CONSTRAINT ck_persons_sex CHECK (sex IN ('MALE', 'FEMALE'))
);

CREATE INDEX idx_persons_last_name ON persons (last_name, first_name);
