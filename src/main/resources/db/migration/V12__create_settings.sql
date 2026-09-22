-- V12__create_settings.sql
--
-- Como se identifican los estudiantes es una decision del centro, no una
-- constante del codigo, asi que vive en la base y se edita desde la API.
--
-- La tabla tiene una sola fila y la columna singleton es lo que lo hace
-- imposible de romper: un UNIQUE sobre una columna que un CHECK obliga a ser
-- TRUE no admite una segunda fila. La fila se siembra aqui para que el
-- servicio sea una lectura y no tenga que decidir entre crear y actualizar,
-- y para que el valor por defecto este escrito en un solo sitio.
--
-- singleton no se mapea en la entidad a proposito: es un invariante de la
-- base, no estado del dominio, y no mapearlo impide que Java inserte otra.

CREATE TABLE settings (
    id                        UUID PRIMARY KEY,
    singleton                 BOOLEAN     NOT NULL DEFAULT TRUE,
    student_identifier        VARCHAR(20) NOT NULL,
    self_registration_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMPTZ NOT NULL,
    updated_at                TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_settings_singleton UNIQUE (singleton),
    CONSTRAINT ck_settings_singleton CHECK (singleton),
    CONSTRAINT ck_settings_student_identifier CHECK (
        student_identifier IN ('DOCUMENT_NUMBER', 'EMAIL', 'MANUAL'))
);

INSERT INTO settings (id, student_identifier, created_at, updated_at)
VALUES (gen_random_uuid(), 'DOCUMENT_NUMBER', now(), now());
