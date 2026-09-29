-- V18__create_attachments.sql
--
-- Una tarea y una entrega pueden llevar varios adjuntos, archivo o enlace.
-- Cada adjunto es de una tarea (lo sube el docente) o de una entrega (lo sube
-- el estudiante), nunca de las dos: dos claves foraneas y un CHECK en vez de
-- una columna polimorfica, para que la base siga guardando la integridad.
--
-- Sustituye a las columnas sueltas attachment_key y storage_key, que admitian
-- un solo archivo. Lo que hubiera en ellas pasa a ser un adjunto; su tipo y
-- tamano no se conocen, por eso esas dos columnas admiten nulo.

CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    assignment_id UUID,
    submission_id UUID,
    kind VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255),
    external_url VARCHAR(2048),
    content_type VARCHAR(150),
    size_bytes BIGINT,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_attachments_assignment_id FOREIGN KEY (assignment_id)
        REFERENCES assignments (id),
    CONSTRAINT fk_attachments_submission_id FOREIGN KEY (submission_id)
        REFERENCES submissions (id),
    CONSTRAINT uk_attachments_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_attachments_owner CHECK (num_nonnulls(assignment_id, submission_id) = 1),
    CONSTRAINT ck_attachments_kind CHECK (kind IN ('FILE', 'LINK')),
    CONSTRAINT ck_attachments_source CHECK (
        (kind = 'FILE' AND storage_key IS NOT NULL AND external_url IS NULL)
        OR (kind = 'LINK' AND external_url IS NOT NULL AND storage_key IS NULL))
);

CREATE INDEX idx_attachments_assignment_id ON attachments (assignment_id, position);
CREATE INDEX idx_attachments_submission_id ON attachments (submission_id, position);

INSERT INTO attachments (id, assignment_id, kind, title, storage_key, position, created_at,
        updated_at)
SELECT gen_random_uuid(), id, 'FILE', substring(attachment_key FROM '[^/]*$'), attachment_key,
       1, now(), now()
FROM assignments
WHERE attachment_key IS NOT NULL;

INSERT INTO attachments (id, submission_id, kind, title, storage_key, position, created_at,
        updated_at)
SELECT gen_random_uuid(), id, 'FILE', substring(storage_key FROM '[^/]*$'), storage_key,
       1, now(), now()
FROM submissions
WHERE storage_key IS NOT NULL;

ALTER TABLE submissions DROP CONSTRAINT ck_submissions_content;
ALTER TABLE submissions DROP COLUMN storage_key;
ALTER TABLE assignments DROP COLUMN attachment_key;
