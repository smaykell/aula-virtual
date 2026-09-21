-- V6__migrate_accounts_to_persons.sql
--
-- Traslada el modelo anterior (una cuenta con un rol, los datos de persona
-- dentro de teachers) al nuevo: una persona, N perfiles, una cuenta.
--
-- Las filas que ya existian no tienen documento, asi que se les siembra uno
-- marcado PEND... que no pasa la validacion de la aplicacion: es deliberado,
-- obliga a corregirlo desde /me o desde la ficha antes de volver a guardar.

INSERT INTO persons (
    id, document_type, document_number, first_name, last_name, birth_date, sex,
    created_at, updated_at)
SELECT t.id,
       'DNI',
       'PEND' || substr(replace(t.id::text, '-', ''), 1, 12),
       t.first_name,
       t.last_name,
       t.birth_date,
       t.sex,
       t.created_at,
       now()
FROM teachers t;

INSERT INTO persons (
    id, document_type, document_number, first_name, last_name, birth_date, sex,
    created_at, updated_at)
SELECT u.id,
       'DNI',
       'PEND' || substr(replace(u.id::text, '-', ''), 1, 12),
       'Pendiente',
       'Pendiente',
       DATE '1900-01-01',
       'MALE',
       u.created_at,
       now()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM teachers t WHERE t.user_id = u.id);

ALTER TABLE users ADD COLUMN person_id UUID;

UPDATE users u
SET person_id = COALESCE((SELECT t.id FROM teachers t WHERE t.user_id = u.id), u.id);

ALTER TABLE users ALTER COLUMN person_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_person_id UNIQUE (person_id);
ALTER TABLE users ADD CONSTRAINT fk_users_person_id FOREIGN KEY (person_id)
    REFERENCES persons (id);

INSERT INTO administrators (id, person_id, role, active, created_at, updated_at)
SELECT gen_random_uuid(), u.person_id, u.role, u.active, now(), now()
FROM users u
WHERE u.role IN ('SUPER_ADMIN', 'ADMIN');

ALTER TABLE teachers ADD COLUMN person_id UUID;
UPDATE teachers SET person_id = id;
ALTER TABLE teachers ALTER COLUMN person_id SET NOT NULL;
ALTER TABLE teachers ADD CONSTRAINT uk_teachers_person_id UNIQUE (person_id);
ALTER TABLE teachers ADD CONSTRAINT fk_teachers_person_id FOREIGN KEY (person_id)
    REFERENCES persons (id);

ALTER TABLE teachers DROP CONSTRAINT fk_teachers_user_id;
ALTER TABLE teachers DROP CONSTRAINT uk_teachers_user_id;
ALTER TABLE teachers DROP CONSTRAINT ck_teachers_sex;
ALTER TABLE teachers DROP COLUMN user_id;
ALTER TABLE teachers DROP COLUMN first_name;
ALTER TABLE teachers DROP COLUMN last_name;
ALTER TABLE teachers DROP COLUMN birth_date;
ALTER TABLE teachers DROP COLUMN sex;

DROP INDEX idx_users_role;
ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users DROP COLUMN role;
ALTER TABLE users DROP COLUMN active;
